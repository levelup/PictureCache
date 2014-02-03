/**
 * 
 */
package com.levelup.picturecache.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.FloatMath;

import com.levelup.picturecache.BuildConfig;
import com.levelup.picturecache.IPictureLoaderTransforms;
import com.levelup.picturecache.LifeSpan;
import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.NetworkLoader;
import com.levelup.picturecache.PictureCache;
import com.levelup.picturecache.PictureJob;
import com.levelup.picturecache.UIHandler;
import com.levelup.picturecache.loaders.ViewLoader;
import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

public class PictureJobList implements Runnable {

	private static final boolean DEBUG_BITMAP_DOWNLOADER = false;

	private static class DownloadTarget implements IPictureLoaderTransforms {
		final PictureJob job;
		File fileInCache;
		DownloadTarget(PictureJob job) {
			this.job = job;
		}

		@Override
		public boolean equals(Object o) {
			if (this==o) return true;
			if (!(o instanceof DownloadTarget)) return false;
			DownloadTarget d = (DownloadTarget) o;
			return job.key.equals(d.job.key) && job.mDisplayHandler.equals(d.job.mDisplayHandler);
		}

		@Override
		public int hashCode() {
			return (job.key.hashCode() * 31 + job.mDisplayHandler.hashCode()) * 31 + (null==job.mTransformHandler ? 0 : job.mTransformHandler.hashCode());
		}

		@Override
		public String toString() {
			return "DownloadTarget:"+job.mDisplayHandler+':'+job.mTransformHandler;
		}

		@Override
		public StorageTransform getStorageTransform() {
			return null==job.mTransformHandler ? null : job.mTransformHandler.getStorageTransform();
		}

		@Override
		public BitmapTransform getDisplayTransform() {
			return null==job.mTransformHandler ? null : job.mTransformHandler.getDisplayTransform();
		}
	}

	final String url;
	final NetworkLoader networkLoader;
	final PictureCache mCache;
	final CopyOnWriteArrayList<DownloadTarget> mTargetJobs = new CopyOnWriteArrayList<DownloadTarget>();
	final DownloadManager mMonitor;

	// locked by mTargets
	/** see {@link LifeSpan} values */
	private LifeSpan mLifeSpan;
	private long mItemDate;

	private boolean mCanDownload;
	private boolean mAborting;

	private static final int CONNECT_TIMEOUT_DL = 10000; // 10s

	PictureJobList(PictureJob job, PictureCache cache, DownloadManager monitor) {
		if (job.url==null) throw new NullPointerException("How are we supposed to download a null URL?");
		this.url = job.url;
		this.networkLoader = job.networkLoader;
		this.mCache = cache;
		this.mMonitor = monitor;
	}

	LifeSpan getLifeSpan() {
		return mLifeSpan;
	}
	long getItemDate() {
		return mItemDate;
	}

	@Override
	public String toString() {
		return "BitmapLoader:"+url+"@"+super.hashCode();
	}

	public void run() {
		//LogManager.getLogger().v( "start image load in cache: " + mURL);
		final HashMap<CacheKey,Drawable> targetBitmaps = new HashMap<CacheKey, Drawable>();
		final HashMap<CacheVariant,Drawable> targetNewBitmaps = new HashMap<CacheVariant, Drawable>();
		File downloadToFile = null;
		boolean downloaded = false;
		try {
			BitmapFactory.Options tmpFileOptions = new BitmapFactory.Options();
			tmpFileOptions.inJustDecodeBounds = false;

			for (int i=0;i<mTargetJobs.size();++i) {
				DownloadTarget target = mTargetJobs.get(i);
				checkAbort();

				target.fileInCache = mCache.getCachedFile(target.job.key);
				boolean bitmapWasInCache = target.fileInCache!=null;
				if (!bitmapWasInCache) {
					// we can't use the older version, download the file and create the stored file again
					if (target.fileInCache!=null)
						target.fileInCache.delete();
					target.fileInCache = mCache.getCachedFilepath(target.job.key);
					if (downloadToFile==null && mCanDownload) {
						if (target.getStorageTransform()==null)
							downloadToFile = target.fileInCache;
						else
							downloadToFile = new File(mCache.getAvailaibleTempDir(), "tmp_"+target.job.key.getFilename());
					}
				}

				if (target.fileInCache!=null) {
					Drawable displayDrawable;
					if (!bitmapWasInCache) {
						displayDrawable = null;
					} else if (mCache.getBitmapCache()!=null) {
						displayDrawable = mCache.getBitmapCache().put(keyToBitmapCacheKey(target.job.key, url, target), target.fileInCache, getOutputOptions(tmpFileOptions.outWidth, tmpFileOptions.outHeight, target.job.key));
					} else {
						displayDrawable = new BitmapDrawable(mCache.getContext().getResources(), target.fileInCache.getAbsolutePath());
					}

					if (displayDrawable==null) {
						// we don't have that final file yet, use the download file to generate it
						displayDrawable = targetBitmaps.get(target.job.key);
						if (displayDrawable==null) {
							displayDrawable = loadResourceDrawable(url);

							if (displayDrawable!=null) {
								if (target.getStorageTransform()!=null)
									displayDrawable = new BitmapDrawable(target.getStorageTransform().transformBitmapForStorage(ViewLoader.drawableToBitmap(displayDrawable)));
								else
									bitmapWasInCache = true; // do not store the drawable as a bitmap as it is equal to the source
							}
						}

						if (displayDrawable==null && downloadToFile!=null && !downloaded) {
							try {
								downloadInTempFile(downloadToFile);
								// we need the dimensions of the downloaded file
								tmpFileOptions.inJustDecodeBounds = true;
								BitmapFactory.decodeFile(downloadToFile.getAbsolutePath(), tmpFileOptions);
								if (DEBUG_BITMAP_DOWNLOADER && tmpFileOptions.outHeight <= 0) LogManager.getLogger().i(PictureCache.LOG_TAG, this+" failed to get dimensions from "+downloadToFile);
								downloaded = true;
							} finally {
								if (!downloaded && downloadToFile!=target.fileInCache)
									downloadToFile.delete();
							}
							checkAbort();
						}

						if (downloaded) {
							Bitmap bitmap;
							if (mCache.getBitmapCache()!=null) {
								CacheableBitmapDrawable cachedDrawable = mCache.getBitmapCache().put(keyToBitmapCacheKey(target.job.key, url, target), downloadToFile, getOutputOptions(tmpFileOptions.outWidth, tmpFileOptions.outHeight, target.job.key));
								bitmap = cachedDrawable.getBitmap();
							} else {
								bitmap = BitmapFactory.decodeFile(downloadToFile.getAbsolutePath(), getOutputOptions(tmpFileOptions.outWidth, tmpFileOptions.outHeight, target.job.key));
							}
							if (bitmap!=null) {
								int finalHeight = target.job.key.getBitmapHeight(bitmap.getWidth(), bitmap.getHeight());
								if (finalHeight!=0 && finalHeight != bitmap.getHeight()) {
									//LogManager.getLogger().v(" source size:"+bmp.getWidth()+"x"+bmp.getHeight());
									Bitmap newBmp = Bitmap.createScaledBitmap(bitmap, (bitmap.getWidth() * finalHeight) / bitmap.getHeight(), finalHeight, true);
									/*if (bitmap!=newBmp)
										bitmap.recycle();*/
									bitmap = newBmp;
								}

								if (target.getStorageTransform()!=null)
									bitmap = target.getStorageTransform().transformBitmapForStorage(bitmap);

								displayDrawable = new BitmapDrawable(mCache.getContext().getResources(), bitmap);
							}
						}
					}

					if (displayDrawable!=null) {
						targetBitmaps.put(target.job.key, displayDrawable);
						if (!bitmapWasInCache) {
							CacheVariant variant = new CacheVariant(target.fileInCache, target.job.key);
							targetNewBitmaps.put(variant, displayDrawable);
						}
					} else {
						if (DEBUG_BITMAP_DOWNLOADER) LogManager.getLogger().d(PictureCache.LOG_TAG, this+" failed to get a bitmap for:"+target);
						targetBitmaps.remove(target.job.key);
					}
				}

				if (DEBUG_BITMAP_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, this+" target:"+target+" fileInCache:"+target.fileInCache+" bitmap:"+targetBitmaps.get(target.job.key));
			}

			downloadToFile = null;
		} catch (OutOfMemoryError e) {
			mCache.getOutOfMemoryHandler().onOutOfMemoryError(e);
			LogManager.getLogger().e(PictureCache.LOG_TAG, "Failed to load " + url, e);
			/*} catch (InterruptedException e) {
			LogManager.getLogger().e(PictureCache.TAG, "Interrupted while loading " + mURL, e);*/
		} catch (DownloadFailureException e) {
			// do nothing
		} catch (Throwable e) {
			LogManager.getLogger().e(PictureCache.LOG_TAG, "exception on "+url, e);
		} finally {
			synchronized (mTargetJobs) {
				if (DEBUG_BITMAP_DOWNLOADER) LogManager.getLogger().e(PictureCache.LOG_TAG, this+" finished loading targets:"+mTargetJobs+" bitmaps:"+targetBitmaps);

				mAborting = true; // after this point new targets are not OK for this job
			}

			UIHandler.instance.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// tell the monitor we are done
					//LogManager.getLogger().i(PictureCache.TAG, "finished download thread for " + mURL + " bmp:"+bmp + " rbmp:"+rbmp);
					//LogManager.getLogger().i(PictureCache.TAG, "send display bitmap "+mURL+" aborted:"+abortRequested.get()+" size:"+reqTargets.size());
					//LogManager.getLogger().i(PictureCache.TAG, "ViewUpdate loop "+mURL+" aborted:"+abortRequested.get()+" size:"+reqTargets.size()+" bmp:"+bmp+" rbmp:"+rbmp);
					synchronized (mTargetJobs) {
						for (DownloadTarget target : mTargetJobs) {
							//LogManager.getLogger().i(PictureCache.TAG, false, "ViewUpdate "+mURL);
							Drawable drawable = targetBitmaps.get(target.job.key);

							if (DEBUG_BITMAP_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, this+" display "+drawable+" in "+target.job.mDisplayHandler+" file:"+target.fileInCache+" key:"+target.job.key);
							if (DEBUG_BITMAP_DOWNLOADER) LogManager.getLogger().v(PictureCache.LOG_TAG, this+"  targets:"+mTargetJobs+" bitmaps:"+targetBitmaps);
							//LogManager.getLogger().i(PictureCache.TAG, "display "+mURL+" in "+j+" abort:"+abortRequested);
							if (drawable!=null) {
								Bitmap bitmap = ViewLoader.drawableToBitmap(drawable);
								if (target.getDisplayTransform()!=null)
									bitmap = target.getDisplayTransform().transformBitmap(bitmap);

								Drawable cacheableBmp;
								if (drawable instanceof BitmapDrawable && ((BitmapDrawable) drawable).getBitmap()==bitmap)
									cacheableBmp = drawable;
								else
									cacheableBmp = new BitmapDrawable(mCache.getContext().getResources(), bitmap);
								target.job.mDisplayHandler.drawBitmap(cacheableBmp, url, target.job.drawCookie, mCache.getBitmapCache(), false);
							} else
								target.job.mDisplayHandler.drawErrorPicture(url, mCache.getBitmapCache());
						}
						mTargetJobs.clear();
					}
				}
			});

			if (mMonitor!=null)
				mMonitor.onJobFinishedWithNewBitmaps(this, targetNewBitmaps);

			if (downloadToFile!=null)
				downloadToFile.delete();
		}
	}

	/**
	 * Add a handler for when the URL is downloaded and start the download+processing if it wasn't started
	 * @param job
	 * @return {@code false} is the job was not added to this target (if the download is aborting)
	 */
	boolean addJob(PictureJob job) {
		if (BuildConfig.DEBUG && !job.url.equals(url)) throw new InvalidParameterException(this+" wrong job URL "+job);

		DownloadTarget newTarget = new DownloadTarget(job);
		//LogManager.getLogger().i(PictureCache.TAG, "add recipient view "+view+" for " + mURL);
		if (DEBUG_BITMAP_DOWNLOADER) LogManager.getLogger().e(PictureCache.LOG_TAG, this+" addTarget "+job.mDisplayHandler+" key:"+job.key);
		synchronized (mTargetJobs) {
			if (mAborting) {
				if (DEBUG_BITMAP_DOWNLOADER) LogManager.getLogger().w(PictureCache.LOG_TAG, this+ " is aborting");
				return false;
			}

			if (mTargetJobs.contains(newTarget)) {
				// TODO: update the rounded/rotation status
				if (DEBUG_BITMAP_DOWNLOADER) LogManager.getLogger().d(PictureCache.LOG_TAG, this+" target "+newTarget+" already pending");
				return true;
			}
			mTargetJobs.add(newTarget);

			mCanDownload |= job.mConcurrencyHandler.isDownloadAllowed();

			if (mItemDate < job.mFreshDate)
				mItemDate = job.mFreshDate;

			if (mLifeSpan==null)
				mLifeSpan = job.mLifeSpan;
			else if (mLifeSpan.compare(job.mLifeSpan)<0)
				mLifeSpan = job.mLifeSpan;
		}
		return true;
	}

	boolean removeJob(PictureJob job) {
		boolean deleted = false;

		synchronized (mTargetJobs) {
			if (DEBUG_BITMAP_DOWNLOADER) LogManager.getLogger().e(PictureCache.LOG_TAG, this+" removeTarget "+job);
			for (int i=0;i<mTargetJobs.size();++i) {
				if (mTargetJobs.get(i).job.mDisplayHandler.equals(job.mDisplayHandler)) {
					deleted = mTargetJobs.remove(i)!=null;
					break;
				}
			}
		}

		if (DEBUG_BITMAP_DOWNLOADER) LogManager.getLogger().e(PictureCache.LOG_TAG, this+" removeTarget "+job+" = "+deleted+" remains:"+mTargetJobs.size());
		/*if (deleted) {
			//LogManager.getLogger().v(" deleted job view:"+target+" for "+mURL);
			//target.setLoadingURL(mCache, mURL);
			job.mDisplayHandler.drawDefaultPicture(url, mCache.getBitmapCache());
		}*/
		//else LogManager.getLogger().i(PictureCache.TAG, " keep downloading URL:" + mURL + " remaining views:" + reqViews.size() + " like view:"+reqViews.get(0));
		return deleted;
	}

	private BitmapFactory.Options getOutputOptions(int srcWidth, int srcHeight, CacheKey key) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		if (srcHeight <= 0) {
			LogManager.getLogger().i(PictureCache.LOG_TAG, "could not get the dimension for " + url+" use raw decoding");
		} else {
			int finalHeight = key.getBitmapHeight(srcWidth, srcHeight);

			if (finalHeight>0 && srcHeight > finalHeight*2) {
				//LogManager.getLogger().e(PictureCache.TAG, " Picture scaling by: " + scale +" from Height:" + opts.outHeight + " to "+finalHeight+" for "+mURL);
				opts.inSampleSize = (int) FloatMath.floor((float)srcHeight / finalHeight);
			}
			else opts.inSampleSize = 1;
		}
		//opts.inInputShareable = true;
		//opts.inPurgeable = true;

		return opts;
	}

	private static class AbortDownload extends DownloadFailureException {
		private static final long serialVersionUID = 5568245153235248681L;
	}

	/**
	 * @throws AbortDownload if we should not download or decode any further
	 */
	private void checkAbort() throws AbortDownload {
		synchronized (mTargetJobs) {
			if (mTargetJobs.isEmpty()) {
				if (DEBUG_BITMAP_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, this+ " no more targets, aborting");
				mAborting = true;
				throw new AbortDownload();
			}
		}
	}

	protected boolean isEmpty() {
		synchronized (mTargetJobs) {
			return mTargetJobs.isEmpty();
		}
	}

	private void downloadInTempFile(File tmpFile) throws DownloadFailureException {
		//LogManager.getLogger().i(PictureCache.TAG, "loading "+mURL);
		InputStream is = null;
		try {
			try {
				is = mCache.getContext().getContentResolver().openInputStream(Uri.parse(url));
				//LogManager.getLogger().v("using the content resolver for "+mURL);
			} catch (FileNotFoundException e) {
				//LogManager.getLogger().d(PictureCache.TAG, false, "cache error trying ContentResolver on "+mURL);
				if (null!=networkLoader)
					is = networkLoader.loadURL(url);

				if (null==is) {
					URL url = new URL(this.url);
					URLConnection conn = url.openConnection();
					conn.setConnectTimeout(CONNECT_TIMEOUT_DL);
					conn.setUseCaches(false);
					conn.setRequestProperty("Accept-Encoding", "identity");
					//LogManager.getLogger().e(PictureCache.TAG, conn.getContentEncoding()+" encoding for "+mURL);
					checkAbort();
					try {
						is = conn.getInputStream();
					} catch (FileNotFoundException ee) {
						throw new DownloadFailureException("cache URL not found "+url, e);
					} catch (Exception ee) {
						throw new DownloadFailureException("cache error opening "+url, e);
					}
				}
			}

			if (is==null) {
				throw new DownloadFailureException("impossible to get a stream for "+url);
			}

			checkAbort();
			// store the stream in a temp file
			BufferedInputStream bis = new BufferedInputStream(is);
			FileOutputStream out = new FileOutputStream(tmpFile);
			try {
				byte[] data = new byte[1422];
				int readAmount = is.read(data);
				while (readAmount >= 0) {
					out.write(data, 0, readAmount);
					checkAbort();
					readAmount = is.read(data);
				}
			} finally {
				bis.close();
				out.flush();
				out.close();
			}

			//LogManager.getLogger().v(" got direct:"+bmp);
		} catch (MalformedURLException e) {
			throw new DownloadFailureException("bad URL " + url, e);
		} catch (UnknownHostException e) {
			throw new DownloadFailureException("host not found in "+url, e);
		} catch (OutOfMemoryError e) {
			mCache.getOutOfMemoryHandler().onOutOfMemoryError(e);
			throw new DownloadFailureException("Could not decode image " + url, e);
		} catch (IOException e) {
			throw new DownloadFailureException("Could not read " + url, e);
		} finally {
			try {
				if (is!=null)
					is.close();
			} catch (IOException e) {
				LogManager.getLogger().e(PictureCache.LOG_TAG, "Could not close " + is, e);
			}
		}
	}

	public static String keyToBitmapCacheKey(CacheKey key, String url, IPictureLoaderTransforms transforms) {
		final StringBuilder bitmapKey = new StringBuilder(key.toString());
		bitmapKey.append(url);
		if (transforms != null) {
			if (transforms.getStorageTransform() != null)
				bitmapKey.append(transforms.getStorageTransform().getVariantPostfix());
			if (transforms.getDisplayTransform() != null)
				bitmapKey.append(transforms.getDisplayTransform().getVariant());
		}
		return bitmapKey.toString();
	}

	private String resourcePath;

	private synchronized Drawable loadResourceDrawable(String url) {
		if (resourcePath==null)
			resourcePath = "android.resource://"+mCache.getContext().getPackageName()+"/";
		if (!url.startsWith(resourcePath))
			return null;
		return mCache.getContext().getResources().getDrawable(Integer.valueOf(url.substring(resourcePath.length())));
	}

}