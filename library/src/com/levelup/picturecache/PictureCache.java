package com.levelup.picturecache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.gawst.asyncdb.InMemoryHashmapDb;
import org.gawst.asyncdb.Logger;
import org.gawst.asyncdb.MapEntry;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.BitmapLruCache.Builder;
import uk.co.senab.bitmapcache.BitmapLruCache.RecyclePolicy;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ReceiverCallNotAllowedException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.text.TextUtils;

import com.levelup.picturecache.internal.ApiLevel8;
import com.levelup.picturecache.internal.CacheItem;
import com.levelup.picturecache.internal.CacheKey;
import com.levelup.picturecache.internal.CacheVariant;
import com.levelup.picturecache.internal.DownloadManager;
import com.levelup.picturecache.internal.PictureJobList;
import com.levelup.picturecache.internal.RemoveExpired;
import com.levelup.picturecache.internal.ThreadSafeBitmapLruCache;
import com.levelup.picturecache.loaders.PrecacheImageLoader;
import com.levelup.picturecache.loaders.RemoteViewLoader;
import com.levelup.picturecache.loaders.ViewLoader;
import com.levelup.picturecache.utils.FileUtils;

/**
 * base class to use the picture cache to load images and keep a persistent cache 
 */
public abstract class PictureCache extends InMemoryHashmapDb<CacheKey,CacheItem> {

	public static final String LOG_TAG = "PictureCache";
	public final static boolean DEBUG_CACHE = false & BuildConfig.DEBUG;

	/**
	 * How many new items need to be added to the database before a purge is done
	 */
	private static final int MIN_ADD_BEFORE_PURGE = 7;

	/**
	 * size in bytes of the amount of storage available for files of the specified {@link LifeSpan}
	 * @param lifeSpan type of {@link LifeSpan}
	 * @return the amount available in bytes
	 * @see {@link #notifyStorageSizeChanged()}
	 */
	public abstract int getCacheMaxSize(LifeSpan lifeSpan);

	/**
	 * Return a different uuid for when the original uuid just got a new URL. this way we can keep the old and new versions in the cache
	 * @param uuid base UUID
	 * @param URL old URL
	 * @return different UUID to stored the old cached version, {@code null} if you don't want to deal with this
	 */
	abstract protected String getOldPicUUID(String uuid, String URL);

	/**
	 * @return The app name that will show up in the Gallery or null if you don't plan to use {@link #saveInGallery(String, int, boolean, boolean, int)}
	 */
	abstract protected String getAppName();

	/**
	 * Get the path for a previously used cache folder that you would like to be erased
	 */
	protected String getOldCacheFolder() {
		return null;
	}

	private static final String DATABASE_NAME = "PictureCachev2.sqlite";
	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_NAME = "Pictures";

	private static final String CREATE_TABLE = 
			"CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " " + 
					"(UUID VARCHAR, " +                  // key: the unique ID representing this item in the DB
					"SRC_URL VARCHAR not null, " +       // the source URL
					"TYPE INTEGER DEFAULT 0, " +         // the type of URL (short term 0 / long term 1 / eternal 2)
					"PATH VARCHAR, " +                   // the path in the cached picture file
					"REMOTE_DATE LONG DEFAULT 0, " +     // the last remote date using to the item (if applicable)
					"DATE LONG not null DEFAULT -1, " +  // the date of last access to the item
					"PRIMARY KEY (UUID));";

	private Boolean hasAssertedDirectory = Boolean.FALSE;

	private final OutOfMemoryHandler ooHandler;

	private final DownloadManager mJobManager;
	private final Context mContext;

	private final ThreadSafeBitmapLruCache mBitmapCache;

	private File mCacheFolder;

	private final AtomicInteger mPurgeCounterLongterm = new AtomicInteger();
	private final AtomicInteger mPurgeCounterShortterm = new AtomicInteger();

	@Override
	protected final String getMainTableName() {
		return TABLE_NAME;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);
	}

	@Override
	protected MapEntry<CacheKey, CacheItem> getEntryFromCursor(Cursor c) {
		int indexPath = c.getColumnIndex("PATH");
		int indexURL = c.getColumnIndex("SRC_URL");
		int indexType = c.getColumnIndex("TYPE");
		int indexRemoteDate = c.getColumnIndex("REMOTE_DATE");
		int indexDate = c.getColumnIndex("DATE");
		int indexUUID = c.getColumnIndex("UUID");

		final String url = c.getString(indexURL);

		if (indexRemoteDate == -1) {
			// updating from an old DB
			indexRemoteDate = c.getColumnIndex("TOUIT_ID");

			int indexPathRounded = c.getColumnIndex("PATHR");
			int indexHeight = c.getColumnIndex("HEIGHT");
			int indexWidthBased = c.getColumnIndex("WIBASE");

			String path = c.getString(indexPath);
			String pathr = c.getString(indexPathRounded);
			boolean widthBased;
			if (indexWidthBased < 0)
				widthBased = false;
			else
				widthBased = c.getInt(indexWidthBased) != 0;

			if (!TextUtils.isEmpty(path)) {
				CacheItem val = new CacheItem(new File(path), url);
				if (val.path.exists()) {
					val.setLifeSpan(LifeSpan.fromStorage(c.getInt(indexType)));
					val.remoteDate = c.getLong(indexRemoteDate);
					val.lastAccessDate = c.getLong(indexDate);

					CacheKey key = CacheKey.newUUIDBasedKey(c.getString(indexUUID), c.getInt(indexHeight), widthBased, StorageType.AUTO, null);

					put(key, val);
				} else {
					if (DEBUG_CACHE) LogManager.logger.d(LOG_TAG, "missing cache file for undated item "+path);
				}
			}

			if (!TextUtils.isEmpty(pathr)) {
				CacheItem val = new CacheItem(new File(pathr), url);
				if (val.path.exists()) {
					val.setLifeSpan(LifeSpan.fromStorage(c.getInt(indexType)));
					val.remoteDate = c.getLong(indexRemoteDate);
					val.lastAccessDate = c.getLong(indexDate);

					CacheKey key = CacheKey.newUUIDBasedKey(c.getString(indexUUID), c.getInt(indexHeight), widthBased, StorageType.AUTO, "_r");

					put(key, val);
				}
			}

			return null; // already done manually
		} else {
			final String serializedKey = c.getString(indexUUID);
			if (TextUtils.isEmpty(serializedKey)) {
				LogManager.logger.w(LOG_TAG, "trying to load an empty cache key for "+url);
				return null;
			}
			final CacheKey key = CacheKey.unserialize(serializedKey);
			final String path = c.getString(indexPath);
			if (TextUtils.isEmpty(path)) {
				LogManager.logger.w(LOG_TAG, "trying to load an empty cache item for "+url);
				remove(key); // make sure we don't use it again
				return null;
			}
			File picSrc = new File(path);
			CacheItem val = new CacheItem(picSrc, url);
			val.setLifeSpan(LifeSpan.fromStorage(c.getInt(indexType)));
			val.remoteDate = c.getLong(indexRemoteDate);
			val.lastAccessDate = c.getLong(indexDate);
			if (!picSrc.exists() || !picSrc.isFile()) {
				LogManager.logger.w(LOG_TAG, "trying to load a missing file for "+val);
				remove(key); // make sure we don't use it again
				return null;
			}

			return new MapEntry<CacheKey, CacheItem>(key, val);
		}
	}

	@Override
	protected ContentValues getValuesFromData(MapEntry<CacheKey, CacheItem> data, SQLiteDatabase dbToFill) throws RuntimeException {
		if (data.getValue().path==null) {
			LogManager.logger.w(LOG_TAG, "cache item has an empty path :"+data.getKey()+" / "+data.getValue());
			throw new RuntimeException("empty path for "+data);
		}

		ContentValues values = new ContentValues(6);
		values.put("UUID", data.getKey().serialize());
		values.put("SRC_URL", data.getValue().URL);
		values.put("TYPE", data.getValue().getLifeSpan().toStorage());
		values.put("PATH", data.getValue().path.getAbsolutePath());
		values.put("REMOTE_DATE", data.getValue().remoteDate);
		values.put("DATE", data.getValue().lastAccessDate);

		return values;
	}

	@Override
	protected String getKeySelectClause(CacheKey key) {
		return "UUID=?";
	}

	@Override
	protected String[] getKeySelectArgs(CacheKey key) {
		return new String[] { key.serialize() };
	}

	/**
	 * Constructor of a PictureCache
	 * <p><b>Do not use multiple instances with this constructor!</b></p>
	 * @param context Context of the application, may also be used to get a {@link ContentResolver}
	 * @param logger A {@link Logger} object used to send all the logs generated inside the cache, may be null
	 * @param ooHandler A {@link OutOfMemoryHandler} object used to notify when we are short on memory, may be null
	 * @param bitmapCacheSize The size to use in memory for the Bitmaps cache, 0 for no memory cache, -1 for heap size based
	 */
	protected PictureCache(Context context, Logger logger, OutOfMemoryHandler ooHandler, int bitmapCacheSize) {
		this(context, logger, ooHandler, bitmapCacheSize, null);
	}

	private static class InitCookie {
		final Context context;
		final String folderName;

		InitCookie(Context context, String folderName) {
			this.context = context;
			this.folderName = folderName;
		}
	}

	/**
	 * Constructor of a PictureCache
	 * @param context Context of the application, may also be used to get a {@link ContentResolver}
	 * @param logger A {@link Logger} object used to send all the logs generated inside the cache, may be null
	 * @param ooHandler A {@link OutOfMemoryHandler} object used to notify when we are short on memory, may be null
	 * @param bitmapCacheSize The size to use in memory for the Bitmaps cache, 0 for no memory cache, -1 for heap size based
	 * @param folderName Storage folder name on the external disk (erased when the app is uninstalled). If you use multiple PictureCache instances you must use a different folder for each instance
	 */
	protected PictureCache(Context context, Logger logger, OutOfMemoryHandler ooHandler, int bitmapCacheSize, String folderName) {
		super(context, null==folderName ? DATABASE_NAME : (folderName+"_pic.sqlite"), DATABASE_VERSION, logger, new InitCookie(context, folderName));

		LogManager.setLogger(logger==null ? new LogManager.LoggerDefault() : logger);
		this.mContext = context;
		if (ooHandler==null)
			this.ooHandler = new OutOfMemoryHandler() {
			// do nothing
			@Override
			public void onOutOfMemoryError(OutOfMemoryError e) {}
		};
		else
			this.ooHandler = ooHandler;

		if (bitmapCacheSize==0)
			this.mBitmapCache = null;
		else {
			Builder builder = new BitmapLruCache.Builder(context).
					setDiskCacheEnabled(false)
					.setMemoryCacheEnabled(true)
					.setRecyclePolicy(RecyclePolicy.DISABLED);
			if (bitmapCacheSize < 0)
				builder.setMemoryCacheMaxSizeUsingHeapSize();
			else
				builder.setMemoryCacheMaxSize(bitmapCacheSize);
			this.mBitmapCache = new ThreadSafeBitmapLruCache(builder.build());
		}

		mJobManager = new DownloadManager(this);

		//getWritableDatabase().setLockingEnabled(false); // we do our own thread protection
	}

	@Override
	protected void preloadInit(Object c, Logger logger) {
		super.preloadInit(c, logger);

		hasAssertedDirectory = false;

		InitCookie cookie = (InitCookie) c;

		File olddir = new File(Environment.getExternalStorageDirectory(), "/Android/data/"+cookie.context.getPackageName()+'/'+(null!=cookie.folderName ? cookie.folderName : "cache"));
		if (olddir.exists())
			mCacheFolder = olddir;
		else {
			File newdir = null;
			try {
				newdir = ApiLevel8.getPrivatePictureDir(cookie.context);
			} catch (VerifyError ignored) {
			} catch (NoSuchFieldError ignored) {
			} catch (NullPointerException ignored) {
			} finally {
				if (newdir == null)
					newdir = olddir;
				else if (null!=cookie.folderName)
					newdir = new File(newdir, cookie.folderName);
			}
			mCacheFolder = newdir;
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		LogManager.logger.w(LOG_TAG, "Upgrading PictureCache from " + oldVersion + " to " + newVersion);
	}

	/**
	 * Get the File path that should be used for this {@code key}
	 * @param key
	 * @return
	 * @throws SecurityException
	 * @throws IOException
	 */
	public File getCachedFilepath(CacheKey key) throws SecurityException, IOException {
		// TODO: handle the switch between phone memory and SD card
		assertFolderExists();
		return new File(mCacheFolder, key.getFilename());
	}

	public File getTempDir()
	{
		try {
			assertFolderExists();
		} catch (SecurityException e) {
			LogManager.logger.e(LOG_TAG, "getTempDir() cannot access the dir ", e);
		} catch (IOException e) {
			LogManager.logger.e(LOG_TAG, "getTempDir() cannot access the dir ", e);
		}
		return mCacheFolder;
	}

	/**
	 * get a directory to store temporary files that should always be available (ie even when the sdcard is not present)
	 * @return
	 */
	public File getAvailaibleTempDir() {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
			return getTempDir();

		return mContext.getCacheDir();
	}

	public File getPictureDir() {
		File dstDir = null;
		String appName = getAppName();
		if (!TextUtils.isEmpty(appName)) {
			try {
				try {
					dstDir = new File(ApiLevel8.getPublicPictureDir(), appName);
				} catch (VerifyError e) {
					dstDir = new File(Environment.getExternalStorageDirectory()+"/DCIM", appName);
				} catch (NoSuchFieldError e) {
					dstDir = new File(Environment.getExternalStorageDirectory()+"/DCIM", appName);
				}
				dstDir.mkdirs();
			} catch (SecurityException e) {
				LogManager.logger.e(LOG_TAG, "getPictureDir() cannot access the dir ", e);
			}
		}
		return dstDir;
	}

	private void assertFolderExists() throws IOException, SecurityException {
		//LogManager.logger.e(TAG, "assertFolderExists " +DirAsserted);
		synchronized (hasAssertedDirectory) {
			if (!hasAssertedDirectory) {
				//LogManager.logger.i("data dir=" + Environment.getDataDirectory().getAbsolutePath());
				if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
					//LogManager.logger.w(TAG, "cache dir=" + dir.getAbsolutePath()+" exists:"+dir.exists());
					if (mCacheFolder.exists() && mCacheFolder.isDirectory())
						hasAssertedDirectory = true;
					else {
						hasAssertedDirectory = mCacheFolder.mkdirs();
						//LogManager.logger.w(TAG, "cache dir=" + dir.getAbsolutePath()+" asserted:"+DirAsserted);
						if (hasAssertedDirectory) {
							new File(mCacheFolder, ".nomedia").createNewFile();
						}
					}

					String oldFolder = getOldCacheFolder();
					if (oldFolder != null) {
						final File oldDir = new File(Environment.getExternalStorageDirectory(), oldFolder);
						if (oldDir.exists()) {
							new Thread() {
								public void run() {
									FileUtils.deleteDirectory(oldDir);
								}
							}.start();
						}
					}
				}
			}
		}
	}

	public long getCacheSize(LifeSpan lifeSpan) {
		long result = 0;
		mDataLock.lock();
		try {
			Iterator<Entry<CacheKey, CacheItem>> v = getMap().entrySet().iterator();
			Entry<CacheKey, CacheItem> k;
			while (v.hasNext()) {
				k = v.next();
				if (k.getValue().getLifeSpan()!=lifeSpan) continue;
				result += k.getValue().path.length();
			}
		} catch (Throwable e) {
			// workaround to avoid locking mData during read/write in the DB
			LogManager.logger.e(LOG_TAG, "getCacheSize failed", e);
		} finally {
			mDataLock.unlock();
		}
		return result;
	}

	public Entry<CacheKey, CacheItem> getCacheOldestEntry(LifeSpan lifeSpan) {
		// LogManager.logger.d(TAG, "getCacheOldest in");
		mDataLock.lock();
		try {
			Entry<CacheKey, CacheItem> result = null;
			for (Entry<CacheKey, CacheItem> entry : getMap().entrySet()) {
				final CacheItem item = entry.getValue();
				if (lifeSpan==item.getLifeSpan() && (result==null || result.getValue().lastAccessDate > item.lastAccessDate))
					result = entry;
			}
			// LogManager.logger.e(TAG, "getCacheOldest out with "+result);
			return result;
		} finally {
			mDataLock.unlock();
		}
	}

	/**
	 * 
	 * @param job
	 */
	void doPictureJob(PictureJob job) {
		File file;
		mDataLock.lock();
		try {
			if (DEBUG_CACHE) LogManager.logger.d(LOG_TAG, "getting picture "+job.url+" into "+job.mDisplayHandler+" key:"+job.key);
			CacheItem v = getMap().get(job.key);

			if (TextUtils.isEmpty(job.url) && null!=v) {
				// get the URL matching the UUID if we don't have one set
				job = job.cloneBuilder().setURL(v.URL).build();
				//LogManager.logger.i("no URL specified for "+key+" using "+URL);
			}

			//LogManager.logger.v(TAG, "load "+URL+" in "+target+" key:"+key);
			String wasPreviouslyLoading = job.mConcurrencyHandler.setLoadingURL(job.url); 
			if (null!=job.url && job.url.equals(wasPreviouslyLoading)) {
				if (DEBUG_CACHE) LogManager.logger.v(LOG_TAG, job.mDisplayHandler+" no need to draw anything");
				// TODO if the old job is different than the new one, we need to go ahead and do the new one
				return; // no need to do anything the image is the same or downloading for it
			}

			if (TextUtils.isEmpty(job.url)) {
				LogManager.logger.i(LOG_TAG, "no URL specified/known for "+job.key+" using default");
				cancelPictureJob(job);
				job.mDisplayHandler.drawDefaultPicture(null);
				return;
			}

			if (wasPreviouslyLoading!=null) {
				// cancel the loading of the previous URL for this loader
				mJobManager.removeDownloadTarget(job, wasPreviouslyLoading);
			}

			/*if (URL.startsWith("android.resource://")) {
			URL = URL.substring(19);
			int resId = Integer.valueOf(URL.substring(URL.indexOf('/')+1));
			target.setImageResource(resId);
			return;
		}*/

			//if (URL!=null && !URL.contains("/profile_images/"))
			if (v != null) {
				if (DEBUG_CACHE) LogManager.logger.v(LOG_TAG, job.key+" found cache item "+v+" URL:"+job.url);
				try {
					if (job.url != null && !job.url.equals(v.URL)) {
						// the URL for the cached item changed
						if (DEBUG_CACHE) LogManager.logger.v(LOG_TAG, job.key+" changed from "+v.URL+" to "+job.url+" remoteDate:"+v.remoteDate+" was "+job.mFreshDate);
						if (v.remoteDate <= job.mFreshDate) { // '=' favor the newer url when dates are 0
							// the item in the Cache is older than this request, the image changed for a newer one
							// we need to mark the old one as short term with a UUID that has the picture ID inside
							String deprecatedUUID = getOldPicUUID(job.key.UUID, v.URL);
							CacheKey oldVersionKey;
							if (!TextUtils.isEmpty(deprecatedUUID))
								oldVersionKey = job.key.copyWithNewUuid(deprecatedUUID);
							else
								oldVersionKey = job.key.copyWithNewUrl(v.URL);
							// move the current content to the deprecated key
							boolean success = moveCachedFiles(job.key, oldVersionKey, LifeSpan.SHORTTERM);
							if (DEBUG_CACHE) LogManager.logger.v(LOG_TAG, job.key+" moved to "+oldVersionKey+" success:"+success);
						} else {
							// use the old image from the cache with that URL
							String dstUUID = getOldPicUUID(job.key.UUID, job.url);
							final CacheKey newKey;
							if (!TextUtils.isEmpty(dstUUID)) {
								newKey = job.key.copyWithNewUuid(dstUUID);
							} else {
								newKey = job.key.copyWithNewUrl(job.url);
							}
							job = job.cloneBuilder().forceCacheKey(newKey).build();
							if (DEBUG_CACHE) LogManager.logger.v(LOG_TAG, job.key+" will be used for that old version");
						}
					}
				} catch (SecurityException e) {
					LogManager.logger.e(LOG_TAG, "getPicture exception:" + e.getMessage(), e);
				} catch (OutOfMemoryError e) {
					LogManager.logger.w(LOG_TAG, "Could not decode image " + job.url+' '+e);
					ooHandler.onOutOfMemoryError(e);
				}
			}
			//else LogManager.logger.i(key.toString()+" not found in "+mData.size()+" cache elements");

			file = getCachedFile(job.key);
			if (file!=null) {
				if (!file.exists() || !file.isFile()) {
					LogManager.logger.w(LOG_TAG, "File "+file+" disappeared for "+job.key);
					remove(job.key);
					file = null;
				}
			}
		} finally {
			mDataLock.unlock();
		}

		final String bitmapCacheKey = mBitmapCache!=null ? PictureJobList.keyToBitmapCacheKey(job, job.url) : null;
		if (mBitmapCache!=null) {
			BitmapDrawable cachedBmp = mBitmapCache.get(bitmapCacheKey);
			if (cachedBmp!=null) {
				Bitmap bmp = cachedBmp.getBitmap();
				if (bmp!=null) {
					if (null!=job.getDisplayTransform()) {
						Bitmap newBmp = job.getDisplayTransform().transformBitmap(bmp);
						if (newBmp!=bmp)
							cachedBmp = new BitmapDrawable(mContext.getResources(), bmp);
					}
					if (DEBUG_CACHE) LogManager.logger.d(LOG_TAG, "using cached bitmap for URL "+job.url+" key:"+bitmapCacheKey);
					job.mDisplayHandler.drawBitmap(cachedBmp, job.url, job.drawCookie, true);
					return;
				}
				LogManager.logger.w(LOG_TAG, "try to draw bitmap "+job.key+" already recycled in "+job.mDisplayHandler+" URL:"+job.url);
			}
		}

		if (null!=file && !UIHandler.isUIThread() && job.mConcurrencyHandler.canDirectLoad(file)) {
			try {
				if (mBitmapCache!=null) {
					BitmapDrawable cachedBmp = mBitmapCache.put(bitmapCacheKey, file);
					if (cachedBmp!=null) {
						Bitmap bmp = cachedBmp.getBitmap();
						if (bmp!=null) {
							if (null!=job.getDisplayTransform()) {
								Bitmap newBmp = job.getDisplayTransform().transformBitmap(bmp);
								if (newBmp!=bmp)
									cachedBmp = new BitmapDrawable(mContext.getResources(), newBmp);
							}
							if (DEBUG_CACHE) LogManager.logger.d(LOG_TAG, "using direct file for URL "+job.url+" file:"+file);
							job.mDisplayHandler.drawBitmap(cachedBmp, job.url, job.drawCookie, true);
							return;
						}
					}
				} else {
					Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
					if (bmp!=null) {
						if (null!=job.getDisplayTransform())
							bmp = job.getDisplayTransform().transformBitmap(bmp);

						BitmapDrawable cachedBmp = new BitmapDrawable(mContext.getResources(), bmp);
						if (DEBUG_CACHE) LogManager.logger.d(LOG_TAG, "using direct file for URL "+job.url+" file:"+file);
						job.mDisplayHandler.drawBitmap(cachedBmp, job.url, job.drawCookie, true);
						return;
					}
				}
			} catch (OutOfMemoryError e) {
				job.mDisplayHandler.drawDefaultPicture(job.url);
				LogManager.logger.w(LOG_TAG, "can't decode "+file+' '+e);
				ooHandler.onOutOfMemoryError(e);
				return;
			}
		}

		job.mDisplayHandler.drawDefaultPicture(job.url);

		// we could not read from the cache, load the URL
		mJobManager.addDownloadTarget(job);
	}

	/**
	 * Helper method for {@link PictureJob} to load a height based picture using the cache 
	 * @param loader The handler used to display the loaded bitmap/placeholder on the target, see {@link ViewLoader}, {@link RemoteViewLoader} or {@link PrecacheImageLoader}
	 * @param URL The bitmap URL to load into the handler (may be null if UUID is not null)
	 * @param UUID A unique ID representing the element in the cache (may be null if URL is not null)
	 * @param cookie An object that will be passed to the loader when the URL is displayed
	 * @param itemDate The date in which the item was created, this is used to purge images older than this one from the cache
	 * @param lifeSpan How long the item should remain in the cache, can be {@link LifeSpan#SHORTTERM},  {@link LifeSpan#LONGTERM} or {@link LifeSpan#ETERNAL}
	 * @param height The height of the image to store in the cache
	 * @param extensionMode The kind of file type we are loading, can be {@link StorageType#AUTO}, {@link StorageType#PNG} or {@link StorageType#JPEG}
	 */
	public void loadPictureWithFixedHeight(PictureLoaderHandler loader, String URL, String UUID, Object cookie, long itemDate, LifeSpan lifeSpan, int height, StorageType extensionMode) {
		PictureJob pictureJob = new PictureJob.Builder(loader, loader)
		.setURL(URL).setUUID(UUID)
		.setTransforms(loader)
		.setFreshDate(itemDate)
		.setLifeType(lifeSpan)
		.setExtensionMode(extensionMode)
		.setDimension(height, false)
		.setDrawCookie(cookie)
		.build();

		pictureJob.startLoading(this);
	}

	/**
	 * Helper method for {@link PictureJob} to load a width based picture using the cache
	 * @param loader The handler used to display the loaded bitmap/placeholder on the target, see {@link ViewLoader}, {@link RemoteViewLoader} or {@link PrecacheImageLoader}
	 * @param URL The bitmap URL to load into the handler (may be null if UUID is not null)
	 * @param UUID A unique ID representing the element in the cache (may be null if URL is not null)
	 * @param cookie An object that will be passed to the loader when the URL is displayed
	 * @param itemDate The date in which the item was created, this is used to purge images older than this one from the cache
	 * @param lifeSpan How long the item should remain in the cache, can be {@link LifeSpan#SHORTTERM},  {@link LifeSpan#LONGTERM} or {@link LifeSpan#ETERNAL}
	 * @param width The width of the image to store in the cache
	 * @param extensionMode The kind of file type we are loading, can be {@link StorageType#AUTO}, {@link StorageType#PNG} or {@link StorageType#JPEG}
	 */
	public void loadPictureWithMaxWidth(PictureLoaderHandler loader, String URL, String UUID, Object cookie, long itemDate, LifeSpan lifeSpan, int width, StorageType extensionMode) {
		PictureJob pictureJob = new PictureJob.Builder(loader, loader)
		.setTransforms(loader)
		.setURL(URL)
		.setUUID(UUID)
		.setFreshDate(itemDate)
		.setLifeType(lifeSpan)
		.setExtensionMode(extensionMode)
		.setDimension(width, true)
		.setDrawCookie(cookie)
		.build();

		pictureJob.startLoading(this);
	}

	/**
	 * Stop loading for that {@link PictureJob} target, keep the target marked for the previously loading URL
	 * @param job
	 */
	public void cancelPictureJob(PictureJob job) {
		if (job != null)
			mJobManager.removeDownloadTarget(job, job.url);
	}

	public boolean saveInGallery(String URL, String UUID, int width, boolean widthBased, boolean Rounded, StorageType extensionMode) {
		boolean succeeded = false;
		final CacheKey key;
		CacheItem v = null;
		try {
			if (TextUtils.isEmpty(UUID))
				key = CacheKey.newUrlBasedKey(URL, width, widthBased, extensionMode, Rounded?"_r":null);
			else
				key = CacheKey.newUUIDBasedKey(UUID, width, widthBased, extensionMode, Rounded?"_r":null);
			mDataLock.lock();
			try {
				v = getMap().get(key);
				if (v != null && v.path != null) {
					if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
						File dst = new File(getPictureDir(), key.getFilename());
						FileUtils.copyFile(v.path, dst, LOG_TAG);
						succeeded = true;

						try {
							GalleryScanner saver = new GalleryScanner(mContext);
							saver.scan(dst);
						} catch (ReceiverCallNotAllowedException e) {
							LogManager.logger.w(LOG_TAG, "could not start the gallery scanning");
						}
					}
				}
			} finally {
				mDataLock.unlock();
			}
		} catch (SecurityException e) {
			LogManager.logger.w(LOG_TAG, "Failed to copy the file for "+v,e);
			succeeded = false;
		} catch (IOException e) {
			LogManager.logger.w(LOG_TAG, "Failed to copy the file for "+v,e);
			succeeded = false;
		} catch (NoSuchAlgorithmException e) {
			succeeded = false;
		}
		return succeeded;
	}

	@Override
	protected void onDataCleared() {
		super.onDataCleared();
		try {
			FileUtils.deleteDirectory(mCacheFolder);
			hasAssertedDirectory = false;
			assertFolderExists();
		} catch (SecurityException e) {
			LogManager.logger.e(LOG_TAG, "clearCache exception", e);
		} catch (IOException e) {
			LogManager.logger.e(LOG_TAG, "clearCache could not recreate the cache folder", e);
		}
	}

	private CacheItem getCacheItem(String UUID, int Height, boolean widthBased, boolean rounded) {
		CacheKey key = CacheKey.newUUIDBasedKey(UUID, Height, widthBased, StorageType.AUTO, rounded?"_r":null);
		return getMap().get(key);
	}

	protected String getCachePath(String UUID, int height, boolean widthBased, boolean rounded) {
		mDataLock.lock();
		try {
			CacheItem cacheItem = getCacheItem(UUID, height, widthBased, rounded);
			if (cacheItem != null) {
				File file = cacheItem.path;
				if (file != null && file.exists())
					return file.getAbsolutePath();
			}
		} finally {
			mDataLock.unlock();
		}
		return null;
	}

	private boolean moveCachedFiles(CacheKey srcKey, CacheKey dstKey, LifeSpan lifeSpan) {
		if (getMap().containsKey(dstKey)) {
			LogManager.logger.d(LOG_TAG, "item "+dstKey+" already exists in the DB, can't copy "+srcKey);
			return false;
		}

		try {
			CacheItem v = getMap().get(srcKey);
			if (v != null) {
				LogManager.logger.v(LOG_TAG, "Copy "+srcKey+" to "+dstKey);
				File src = v.path;
				if (src != null && src.exists()) {
					File dst = getCachedFilepath(dstKey);
					dst.delete();

					if (src.renameTo(dst)) {
						remove(srcKey); // that key is not valid anymore
						v = v.copyWithNewPath(dst);
						v.setLifeSpan(lifeSpan);
						return put(dstKey, v) != null;
					} else {
						LogManager.logger.e(LOG_TAG, "Failed to rename path "+src+" to "+dst);
					}
					//else LogManager.logger.d(TAG, false, "keep the old version of "+newKey);
				}
			}
		} catch (Throwable e) {
			LogManager.logger.e(LOG_TAG, "failed to copy " + srcKey + " to " + dstKey, e);
		}
		return false;
	}

	/**
	 * indicate that the values returned by {@link #getCacheMaxSize(LifeSpan)} have changed
	 */
	protected void notifyStorageSizeChanged() {
		scheduleCustomOperation(new RemoveExpired());
	}

	public final void onNewBitmapLoaded(Map<CacheVariant,Drawable> newBitmaps, String url, long remoteDate, LifeSpan lifeSpan) {
		// handle the storing and adding to the cache
		// save the bitmap for later use
		long fileSizeAdded = 0;
		for (CacheVariant variant : newBitmaps.keySet()) {
			try {
				if (variant.path.exists())
					variant.path.delete();
				Drawable drawable = newBitmaps.get(variant);
				if (null==drawable) {
					LogManager.logger.i(LOG_TAG, "tried to save a null drawable "+variant.key+" from "+url+" as "+variant.path);
					continue;
				}
				Bitmap bmp = ViewLoader.drawableToBitmap(drawable);
				if (null==bmp) {
					LogManager.logger.i(LOG_TAG, "tried to save a null bitmap "+variant.key+" from "+url+" using "+drawable);
					continue;
				}
				UIHandler.assertNotUIThread();
				FileOutputStream fos = new FileOutputStream(variant.path, false);
				bmp.compress(variant.key.getCompression(), variant.key.getCompRatio(), fos);
				fos.close();

				if (DEBUG_CACHE) LogManager.logger.d(LOG_TAG, "stored "+variant.key+" from "+url+" as "+variant.path); 

				mDataLock.lock();
				try {
					CacheItem val = getMap().get(variant.key);
					if (val != null) {
						if (val.remoteDate < remoteDate)
							val.remoteDate = remoteDate;

						if (val.getLifeSpan().compare(lifeSpan) < 0)
							val.setLifeSpan(lifeSpan);

						val.lastAccessDate = System.currentTimeMillis();
						notifyItemChanged(variant.key);
						/*if (!changed && url.equals(val.URL))
							LogManager.logger.v(TAG, "image " + key.toString()+" unchanged");
						else
							LogManager.logger.v(TAG, "image " + key.toString()+" already exists, adjusting the touitDate:"+val.touitID+" bmpIsNew:"+bmpIsNew+" rbmpIsNew:"+rbmpIsNew+" url:"+url);*/
					} else {
						val = new CacheItem(variant.path, url);
						val.remoteDate = remoteDate;
						val.setLifeSpan(lifeSpan);
						val.lastAccessDate = System.currentTimeMillis();
						//LogManager.logger.v(TAG, "adding image " + key.toString() +" type:"+type+" bmpIsNew:"+bmpIsNew+" rbmpIsNew:"+rbmpIsNew+" url:"+url);
						put(variant.key, val);
					}

					fileSizeAdded += variant.path.length();
				} finally {
					mDataLock.unlock();
				}

				//LogManager.logger.i("saved bmp to "+outFile.getAbsolutePath());
			} catch (IOException e) {
				LogManager.logger.i(LOG_TAG, "failed to save "+url+" as "+variant, e);
			}
		}

		//LogManager.logger.i("BitmapLoaded outFile:"+outFile);
		if (fileSizeAdded != 0) {
			final boolean needsPurge;
			if (lifeSpan == LifeSpan.LONGTERM)
				needsPurge = (mPurgeCounterLongterm.incrementAndGet() > MIN_ADD_BEFORE_PURGE);
			else if (lifeSpan == LifeSpan.SHORTTERM)
				needsPurge = (mPurgeCounterShortterm.incrementAndGet() > MIN_ADD_BEFORE_PURGE);
			else
				needsPurge = false;

			if (needsPurge) {
				if (lifeSpan == LifeSpan.LONGTERM)
					mPurgeCounterLongterm.set(0);
				else if (lifeSpan == LifeSpan.SHORTTERM)
					mPurgeCounterShortterm.set(0);
				scheduleCustomOperation(new RemoveExpired(lifeSpan));
			}
		}
	}

	public File getCachedFile(CacheKey key) {
		//if (URL!=null && !URL.contains("/profile_images/"))
		//LogManager.logger.v(TAG, " getPicture URL:"+URL + " key:"+key);
		if (key != null) {
			mDataLock.lock();
			try {
				CacheItem v = getMap().get(key);

				//if (URL!=null && !URL.contains("/profile_images/"))
				if (DEBUG_CACHE) LogManager.logger.v(LOG_TAG, key+" found cache item "+v);
				if (null!=v && null!=v.path && v.path.exists() && v.path.isFile()) {
					return v.path;
				}
				//else LogManager.logger.i(key.toString()+" not found in "+mData.size()+" cache elements");
			} finally {
				mDataLock.unlock();
			}
		}
		return null;
	}

	@Override
	protected void startLoadingFromCursor(Cursor c) {
		try {
			assertFolderExists();
		} catch (SecurityException e) {
			LogManager.logger.e(LOG_TAG, "can't use Cache folder", e);
		} catch (IOException e) {
			LogManager.logger.e(LOG_TAG, "can't use Cache folder", e);
		}
		super.startLoadingFromCursor(c);
	}

	public Context getContext() {
		return mContext;
	}

	public File getCacheFolder() {
		return mCacheFolder;
	}

	public ThreadSafeBitmapLruCache getBitmapCache() {
		return mBitmapCache;
	}

	public OutOfMemoryHandler getOutOfMemoryHandler() {
		return ooHandler;
	}
}
