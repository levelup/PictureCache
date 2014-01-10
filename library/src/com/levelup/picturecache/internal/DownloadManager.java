/**
 * 
 */
package com.levelup.picturecache.internal;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.levelup.picturecache.LifeSpan;
import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.NetworkLoader;
import com.levelup.picturecache.PictureCache;
import com.levelup.picturecache.PictureLoaderHandler;

public class DownloadManager {

	private static final boolean DEBUG_DOWNLOADER = false;

	private final static int THREAD_POOL_SIZE = 2;

	private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(20);

	private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 1, TimeUnit.SECONDS, sPoolWorkQueue) {
		@Override
		protected void beforeExecute(Thread t, Runnable r) {
			super.beforeExecute(t, r);
			if (r instanceof BitmapDownloader) {
				t.setName("PictureDL-"+((BitmapDownloader) r).mURL.hashCode());
			} else {
				t.setName("PictureDL-"+t);
			}
			t.setPriority(Thread.MIN_PRIORITY);
		}
	};

	private final Hashtable<String, BitmapDownloader> mJobs = new Hashtable<String, BitmapDownloader>();
	private final PictureCache mCache;

	public DownloadManager(PictureCache pictureCache) {
		this.mCache = pictureCache;
	}

	public void addDownloadTarget(PictureCache cache, String URL, Object cookie, PictureLoaderHandler loadHandler, CacheKey key, long itemDate, LifeSpan lifeSpan, NetworkLoader networkLoader) {
		// find out if that URL is already loading, if so add the view to the recipient
		synchronized (mJobs) {
			// add job by URL
			BitmapDownloader downloader = mJobs.get(URL);
			if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, "add loader:"+loadHandler+" to downloader:"+downloader);
			final boolean targetAdded = downloader!=null && downloader.addTarget(loadHandler, key, itemDate, lifeSpan);
			if (!targetAdded) {
				if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, "add new downloader for "+URL+" key:"+key+" loader:"+loadHandler+" jobs:"+mJobs);
				// create a fresh new one if an old one is not ready to accept our loadHandler
				downloader = new BitmapDownloader(URL, networkLoader, cookie, cache, this);
				downloader.addTarget(loadHandler, key, itemDate, lifeSpan);
				try {
					threadPool.execute(downloader);
					mJobs.put(URL, downloader);
				} catch (RejectedExecutionException e) {
					LogManager.getLogger().w(PictureCache.LOG_TAG, "can't execute "+downloader, e);
				}
			}
			if (DEBUG_DOWNLOADER) {
				downloader = mJobs.get(URL);
				LogManager.getLogger().e(PictureCache.LOG_TAG, "downloader for "+URL+" = "+downloader+" loader added:"+targetAdded);
			}
		}
	}

	/**
	 * has to be done before a new {@link PictureLoaderHandler.setLoadingNewURL(DownloadManager, String, SimpleLogger)}
	 * @param loadHandler
	 * @param URL TODO
	 * @return true if there was a task loading
	 */
	public boolean cancelDownloadForLoader(PictureLoaderHandler loadHandler, String URL) {
		synchronized (mJobs) {
			if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, "cancelDownloadForLoader for "+URL+" loadHandler:"+loadHandler);
			if (!TextUtils.isEmpty(URL)) {
				BitmapDownloader downloader = mJobs.get(URL);
				if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, " cancelDownloadForLoader loadHandler:"+loadHandler+" found:"+downloader);
				if (downloader!=null) {
					//LogManager.getLogger().d(PictureCache.TAG, "cancelDownloadForTarget for URL " + URL+" for "+loader);
					boolean removed = downloader.removeTarget(loadHandler);
					if (downloader.isEmpty())
						threadPool.remove(downloader);
					return removed;
				}
			}
		}

		// find the target by view
		//LogManager.getLogger().w(PictureCache.TAG, "cancelDownloadForTarget by key " + loader);
		Enumeration<BitmapDownloader> downloaders = mJobs.elements();
		while (downloaders.hasMoreElements()) {
			BitmapDownloader downloader = downloaders.nextElement();
			if (downloader.removeTarget(loadHandler)) {
				if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, " cancelDownloadForLoader loadHandler:"+loadHandler+" deleted on:"+downloader/*+" url:"+url*/);
				if (downloader.isEmpty())
					threadPool.remove(downloader);
				return true;
			}
		}
		if (DEBUG_DOWNLOADER) LogManager.getLogger().w(PictureCache.LOG_TAG, "cancelDownloadForLoader do nothing for loadHandler:"+loadHandler);
		return false;
	}

	void onJobFinishedWithNewBitmaps(BitmapDownloader downloader, HashMap<CacheVariant,Drawable> newBitmaps) {
		mCache.onNewBitmapLoaded(newBitmaps, downloader.mURL, downloader.getItemDate(), downloader.getLifeSpan());

		synchronized (mJobs) {
			if (mJobs.containsKey(downloader.mURL)) {
				mJobs.remove(downloader.mURL);
				if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, "Job Finishing for "+downloader.mURL + " remaining:"+mJobs);
			}
			else
				LogManager.getLogger().w(PictureCache.LOG_TAG, "Unknown job finishing for "+downloader.mURL + " remaining:"+mJobs);
		}
	}
}