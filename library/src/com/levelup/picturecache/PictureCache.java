package com.levelup.picturecache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import st.gaw.db.InMemoryDbHelper;
import st.gaw.db.InMemoryDbOperation;
import st.gaw.db.InMemoryHashmapDb;
import st.gaw.db.MapEntry;
import android.content.ContentValues;
import android.content.Context;
import android.content.ReceiverCallNotAllowedException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;

import com.levelup.GalleryScanner;
import com.levelup.HandlerUIThread;
import com.levelup.OOMHandler;
import com.levelup.SimpleLogger;
import com.levelup.Utils;
import com.levelup.picturecache.DownloadManager.JobsMonitor;

public abstract class PictureCache extends InMemoryHashmapDb<CacheKey,CacheItem> implements JobsMonitor {

	private static final int MIN_ADD_BEFORE_PURGE = 7;

	/**
	 * return a different uuid for when the original uuid just got a new URL. this way we can keep the old and new versions in the cache
	 * @param uuid base UUID
	 * @param URL old URL
	 * @return different UUID to stored the old cached version
	 */
	abstract protected String getOldPicUUID(String uuid, String URL);

	/**
	 * the App name used to export the pictures in the gallery
	 * @return the app name that will show up in the Gallery or null if you don't plan to use {@link #saveInGallery(String, int, boolean, boolean, int)}
	 */
	abstract protected String getAppName();

	protected String getOldCacheFolder() {
		return null;
	}

	private static final String DATABASE_NAME = "PictureCachev2.sqlite";
	private static final String OLD_DATABASE_NAME = "PictureCache.sqlite"; 
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

	private static Boolean mDirAsserted = Boolean.FALSE;

	private final File mCacheFolder;
	final HandlerUIThread postHandler;

	private int mCacheSizeLongterm;
	private int mCacheSizeEternal;
	private int mCacheSizeShortterm;

	private DownloadManager mJobManager;
	private SimpleLogger mLogger;
	private Context mContext;

	private AtomicInteger mPurgeCounterLongterm = new AtomicInteger();
	private AtomicInteger mPurgeCounterShortterm = new AtomicInteger();

	/**
	 * select the storage format automatically
	 */
	public static final int EXT_MODE_AUTO = 0;
	/**
	 * store the picture in the cache as a JPEG
	 */
	public static final int EXT_MODE_JPEG = 1;
	/**
	 * store the picture in the cache as a PNG
	 */
	public static final int EXT_MODE_PNG  = 2;


	@Override
	protected String getMainTableName() {
		return TABLE_NAME;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);
	}

	@Override
	protected Entry<CacheKey, CacheItem> getEntryFromCursor(Cursor c) {
		int indexPath = c.getColumnIndex("PATH");
		int indexURL = c.getColumnIndex("SRC_URL");
		int indexType = c.getColumnIndex("TYPE");
		int indexRemoteDate = c.getColumnIndex("REMOTE_DATE");
		int indexDate = c.getColumnIndex("DATE");
		int indexUUID = c.getColumnIndex("UUID");

		if (indexRemoteDate==-1) {
			// updating from an old DB
			indexRemoteDate = c.getColumnIndex("TOUIT_ID");

			int indexPathRounded = c.getColumnIndex("PATHR");
			int indexHeight = c.getColumnIndex("HEIGHT");
			int indexWidthBased = c.getColumnIndex("WIBASE");

			String path = c.getString(indexPath);
			String pathr = c.getString(indexPathRounded);
			boolean widthBased;
			if (indexWidthBased<0)
				widthBased = false;
			else
				widthBased = c.getInt(indexWidthBased)!=0;

			if (!TextUtils.isEmpty(path)) {
				CacheItem val = new CacheItem(new File(path), c.getString(indexURL));
				if (val.path.exists()) {
					int typeIdx = c.getInt(indexType);
					if (typeIdx<0) {
						mLogger.w("unknown cache type "+typeIdx);
						val.type = CacheType.CACHE_SHORTTERM;
					} else
						val.type = typeIdx;
					val.remoteDate = c.getLong(indexRemoteDate);
					val.lastAccessDate = c.getLong(indexDate);

					CacheKey key = CacheKey.newUUIDBasedKey(c.getString(indexUUID), c.getInt(indexHeight), widthBased, PictureCache.EXT_MODE_AUTO, null);

					put(key, val);
				}
			}

			if (!TextUtils.isEmpty(pathr)) {
				CacheItem val = new CacheItem(new File(pathr), c.getString(indexURL));
				if (val.path.exists()) {
					int typeIdx = c.getInt(indexType);
					if (typeIdx<0) {
						mLogger.w("unknown cache type "+typeIdx);
						val.type = CacheType.CACHE_SHORTTERM;
					} else
						val.type = typeIdx;
					val.remoteDate = c.getLong(indexRemoteDate);
					val.lastAccessDate = c.getLong(indexDate);

					CacheKey key = CacheKey.newUUIDBasedKey(c.getString(indexUUID), c.getInt(indexHeight), widthBased, PictureCache.EXT_MODE_AUTO, "_r");

					put(key, val);
				}
			}

			return null; // already done manually
		} else {
			final String path = c.getString(indexPath);
			if (TextUtils.isEmpty(path)) {
				mLogger.w("trying to load an empty cache item for "+c.getString(indexURL));
				return null;
			}
			CacheItem val = new CacheItem(new File(path), c.getString(indexURL));
			int typeIdx = c.getInt(indexType);
			if (typeIdx<0) {
				mLogger.w("unknown cache type "+typeIdx);
				val.type = CacheType.CACHE_SHORTTERM;
			} else
				val.type = typeIdx;
			val.remoteDate = c.getLong(indexRemoteDate);
			val.lastAccessDate = c.getLong(indexDate);

			CacheKey key = CacheKey.unserialize(c.getString(indexUUID));

			return new MapEntry<CacheKey, CacheItem>(key, val);
		}
	}

	@Override
	protected ContentValues getValuesFromData(Entry<CacheKey, CacheItem> data, SQLiteDatabase dbToFill) throws RuntimeException {
		if (data.getValue().path==null) {
			mLogger.w("cache item has an empty path :"+data.getKey()+" / "+data.getValue());
			throw new RuntimeException("empty path for "+data);
		}

		ContentValues values = new ContentValues(6);
		values.put("UUID", data.getKey().serialize());
		values.put("SRC_URL", data.getValue().URL);
		values.put("TYPE", data.getValue().type);
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
		return new String[] {key.serialize()};
	}

	protected PictureCache(Context context, HandlerUIThread postHandler, int sizeShortTerm, int sizeLongTerm, int sizeEternal, SimpleLogger logger) {
		super(context, DATABASE_NAME, DATABASE_VERSION);

		this.mContext = context;
		this.mLogger = logger;
		this.postHandler = postHandler;

		File olddir = new File(Environment.getExternalStorageDirectory(), "/Android/data/"+context.getPackageName()+"/cache");
		if (olddir.exists())
			mCacheFolder = olddir;
		else {
			File newdir = null;
			try {
				newdir = ApiLevel8.getPrivatePictureDir(context);
			} catch (VerifyError e) {
			} catch (NoSuchFieldError e) {
			} finally {
				if (newdir==null)
					newdir = olddir;
			}
			mCacheFolder = newdir;
		}

		mJobManager = new DownloadManager(mLogger);
		mJobManager.setMonitor(this);
		mCacheSizeEternal = sizeEternal;
		mCacheSizeLongterm = sizeLongTerm;
		mCacheSizeShortterm = sizeShortTerm;

		File olddb = context.getDatabasePath(OLD_DATABASE_NAME);
		if (olddb.exists()) {
			/* TODO: SQLiteDatabase oldDB = context.openOrCreateDatabase(OLD_DATABASE_NAME, 0, null);
			reloadFromDB(oldDB, TABLE_NAME);
			oldDB.close();
			context.deleteDatabase(OLD_DATABASE_NAME);*/
		}

		//getWritableDatabase().setLockingEnabled(false); // we do our own thread protection
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		mLogger.w("Upgrading PictureCache from " + oldVersion + " to " + newVersion);
	}

	File getCachedFilepath(CacheKey key) throws SecurityException, IOException
	{
		// TODO: handle the switch between phone memory and SD card
		assertFolderExists();
		return new File(mCacheFolder, key.getFilename());
	}

	public File getTempDir()
	{
		try {
			assertFolderExists();
		} catch (SecurityException e) {
			mLogger.e("getTempDir() cannot access the dir ", e);
		} catch (IOException e) {
			mLogger.e("getTempDir() cannot access the dir ", e);
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

		return getContext().getCacheDir();
	}

	public File getPictureDir()
	{
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
				mLogger.e("getPictureDir() cannot access the dir ", e);
			}
		}
		return dstDir;
	}

	private void assertFolderExists() throws IOException, SecurityException 
	{
		//mLogger.e("assertFolderExists " +DirAsserted);
		synchronized (mDirAsserted) {
			if (!mDirAsserted) {
				//mLogger.i("data dir=" + Environment.getDataDirectory().getAbsolutePath());
				if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
					//mLogger.w("cache dir=" + dir.getAbsolutePath()+" exists:"+dir.exists());
					if (mCacheFolder.exists() && mCacheFolder.isDirectory())
						mDirAsserted = Boolean.TRUE;
					else {
						mDirAsserted = Boolean.valueOf(mCacheFolder.mkdirs());
						//mLogger.w("cache dir=" + dir.getAbsolutePath()+" asserted:"+DirAsserted);
						if (mDirAsserted) {
							File noMedia = new File(mCacheFolder, ".nomedia");
							noMedia.createNewFile();
						}
					}

					String oldFolder = getOldCacheFolder();
					if (oldFolder!=null) {
						final File oldDir = new File(Environment.getExternalStorageDirectory(), oldFolder);
						if (oldDir.exists()) {
							new Thread() {
								public void run() {
									deleteDirectory(oldDir);
								}
							}.start();
						}
					}
				}
			}
		}
	}

	private int getCacheMaxSize(int type) {
		if (type == CacheType.CACHE_LONGTERM)
			return mCacheSizeLongterm;
		else if (type == CacheType.CACHE_ETERNAL)
			return mCacheSizeEternal;
		else
			return mCacheSizeShortterm;
	}

	private long getCacheSize(int type) {
		long result = 0;
		mDataLock.lock();
		try {
			Iterator<Entry<CacheKey, CacheItem>> v = getMap().entrySet().iterator();
			Entry<CacheKey, CacheItem> k;
			while (v.hasNext()) {
				k = v.next();
				if (k.getValue().type!=type) continue;
				result += k.getValue().getFileSize();
			}
		} catch (Throwable e) {
			// workaround to avoid locking mData during read/write in the DB
			mLogger.e("getCacheSize failed", e);
		} finally {
			mDataLock.unlock();
		}
		return result;
	}

	private Entry<CacheKey, CacheItem> getCacheOldestEntry(int type) {
		//mLogger.d("getCacheOldest in");
		Entry<CacheKey, CacheItem> result = null;
		for (Entry<CacheKey, CacheItem> k : getMap().entrySet()) {
			if (!CacheType.isStrictlyLowerThan(type, k.getValue().type) && (result==null || result.getValue().lastAccessDate > k.getValue().lastAccessDate))
				result = k;
		}
		//mLogger.e("getCacheOldest out with "+result);
		return result;
	}

	private static class RemoveExpired implements InMemoryDbOperation<Map.Entry<CacheKey,CacheItem>> {

		private final Integer cacheType;

		RemoveExpired() {
			this.cacheType = null;
		}
		RemoveExpired(int cacheType) {
			this.cacheType = cacheType;
		}

		@Override
		public void runInMemoryDbOperation(InMemoryDbHelper<Entry<CacheKey, CacheItem>> db) {
			PictureCache cache = (PictureCache) db;
			if (cacheType!=null)
				makeRoom(cache, cacheType);
			else {
				makeRoom(cache, CacheType.CACHE_SHORTTERM);
				makeRoom(cache, CacheType.CACHE_LONGTERM);
				makeRoom(cache, CacheType.CACHE_ETERNAL);
			}
		}

		private static void makeRoom(PictureCache cache, int type) {
			try {
				long TotalSize = cache.getCacheSize(type);
				int MaxSize = cache.getCacheMaxSize(type);
				if (MaxSize!=0 && TotalSize > MaxSize) {
					// make room in the DB/cache for this new element
					while (TotalSize > MaxSize) {
						//if (type != k.getValue().type) continue;
						//long deleted = 0;
						Entry<CacheKey, CacheItem> entry;
						cache.mDataLock.lock();
						try {
							entry = cache.getCacheOldestEntry(type);
							if (entry==null)
								break;
						} finally {
							cache.mDataLock.unlock();
						}

						CacheItem item = cache.remove(entry.getKey());
						if (item!=null) {
							File f = item.path;
							if (f!=null && f.exists()) {
								long fSize = f.length();
								if (f.delete()) {
									TotalSize -= fSize;
									//deleted += fSize;
								}
							}
						}
						//mLogger.d("makeroom");
					}
				}
			} catch (NullPointerException e) {
				cache.mLogger.w("can't make room for type:"+type,e);
			}
			//mLogger.d("makeroom done");
		}
	}

	/**
	 * 
	 * @param URL
	 * @param key
	 * @param itemDate use to store the previous item for the same {@link key}
	 * @param loader
	 * @param cacheType see {@link CacheType}
	 */
	void getPicture(String URL, CacheKey key, long itemDate, PictureLoaderHandler loader, int cacheType)
	{
		mDataLock.lock();
		try {
			//mLogger.d("getting picture "+URL+" into "+target+" key:"+key);
			if (TextUtils.isEmpty(URL)) {
				// get the URL matching the UUID if we don't have a forced one
				CacheItem v = getMap().get(key);
				if (v!=null)
					URL = v.URL;
				//mLogger.i("no URL specified for "+key+" using "+URL);
			}
			if (TextUtils.isEmpty(URL)) {
				mLogger.d("no URL specified/known for "+key+" using default");
				removePictureLoader(loader, null);
				loader.drawDefaultPicture(null, postHandler, mLogger);
				return;
			}

			//mLogger.v("load "+URL+" in "+target+" key:"+key);
			if (!loader.setLoadingNewURL(mJobManager, URL, mLogger)) {
				//mLogger.v(loader+" no need to draw anything");
				return; // no need to do anything the image is the same or downloading for it
			}

			/*if (URL.startsWith("android.resource://")) {
			URL = URL.substring(19);
			int resId = Integer.valueOf(URL.substring(URL.indexOf('/')+1));
			target.setImageResource(resId);
			return;
		}*/

			File file = getCachedFile(key, URL, itemDate);
			if (file!=null && file.exists() && file.canRead() && loader.canDirectLoad(file)) {
				try {
					Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
					if (bmp!=null) {
						//mLogger.d("using direct file for URL "+URL+" file:"+file);
						loader.drawBitmap(bmp, URL, postHandler, mLogger);
						return;
					}
				} catch (OutOfMemoryError e) {
					loader.drawDefaultPicture(URL, postHandler, mLogger);
					mLogger.w("can't decode "+file,e);
					OOMHandler.handleOutOfMemory(getContext(), postHandler, e);
					return;
				}
			}

			loader.drawDefaultPicture(URL, postHandler, mLogger);

			// we could not read from the cache, load the URL
			if (key!=null)
				mJobManager.addDownloadTarget(this, URL, loader, key, itemDate, cacheType);
		} finally {
			mDataLock.unlock();
		}
	}

	/**
	 * helper method to load a height based picture using the cache
	 * @param handler
	 * @param URL
	 * @param UUID
	 * @param itemDate
	 * @param type how long the item should remain in the cache, can be {@link CacheType#CACHE_SHORTTERM},  {@link CacheType#CACHE_LONGTERM} or {@link CacheType#CACHE_ETERNAL}
	 * @param height
	 * @param extensionMode
	 */
	public void loadPictureWithFixedHeight(PictureLoaderHandler handler, String URL, String UUID, long itemDate, int type, int height, int extensionMode) {
		PictureJobBuilder builder = new PictureJobBuilder(handler);
		builder.setURL(URL);
		builder.setUUID(UUID);
		builder.setFreshDate(itemDate);
		builder.setLifeType(type);
		builder.setExtensionMode(extensionMode);
		builder.setDimension(height, false);
		try {
			builder.startLoading(this);
		} catch (NoSuchAlgorithmException e) {
			mLogger.d("can't load picture", e);
		}
	}

	/**
	 * helper method to load a width based picture using the cache
	 * @param handler
	 * @param URL
	 * @param UUID
	 * @param itemDate
	 * @param type how long the item should remain in the cache, can be {@link CacheType#CACHE_SHORTTERM},  {@link CacheType#CACHE_LONGTERM} or {@link CacheType#CACHE_ETERNAL}
	 * @param width
	 * @param rotation
	 * @param extensionMode
	 */
	public void loadPictureWithMaxWidth(PictureLoaderHandler handler, String URL, String UUID, long itemDate, int type, int width, float rotation, int extensionMode) {
		PictureJobBuilder builder = new PictureJobBuilder(handler);
		builder.setURL(URL);
		builder.setUUID(UUID);
		builder.setFreshDate(itemDate);
		builder.setLifeType(type);
		builder.setExtensionMode(extensionMode);
		builder.setDimension(width, true);
		builder.setRotation(rotation);
		try {
			builder.startLoading(this);
		} catch (NoSuchAlgorithmException e) {
			mLogger.d("can't load picture", e);
		}
	}

	/**
	 * stop loading for that {@link loader} target, keep the target marked for the previously loading URL
	 * @param loader
	 * @param oldURL
	 */
	public void cancelPictureLoader(PictureLoaderHandler loader, String oldURL) {
		if (loader!=null)
			mJobManager.cancelDownloadForLoader(loader, oldURL);
	}

	/**
	 * stop loading for that {@link loader} target, reset loading URL marked on that target
	 * @param loader
	 * @param oldURL
	 */
	public void removePictureLoader(PictureLoaderHandler loader, String oldURL) {
		if (loader!=null) {
			mJobManager.cancelDownloadForLoader(loader, oldURL);
			loader.setLoadingNewURL(mJobManager, null, mLogger);
		}
	}

	public boolean saveInGallery(String UUID, int width, boolean widthBased, boolean Rounded, int extensionMode) throws IOException, SecurityException {
		boolean succeeded = false;
		CacheKey key = CacheKey.newUUIDBasedKey(UUID, width, widthBased, extensionMode, Rounded?"_r":null);
		mDataLock.lock();
		try {
			CacheItem v = getMap().get(key);
			if (v!=null && v.path!=null) {
				if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
					File dst = new File(getPictureDir(), key.getFilename());
					Utils.copyFile(v.path, dst, mLogger);
					succeeded = true;

					try {
						GalleryScanner saver = new GalleryScanner(getContext());
						saver.scan(dst);
					} catch (ReceiverCallNotAllowedException e) {
						mLogger.w("could not start the gallery scanning");
					}
				}
			}
		} finally {
			mDataLock.unlock();
		}
		return succeeded;
	}

	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for(File file : files) {
				if (file.isDirectory())
					deleteDirectory(file);
				else
					file.delete();
			}
			return path.delete();
		}
		return false;
	}

	@Override
	protected void onDataCleared() {
		super.onDataCleared();
		try {
			deleteDirectory(mCacheFolder);
			synchronized (mDirAsserted) {
				mDirAsserted = Boolean.FALSE;
			}
			assertFolderExists();
		} catch (SecurityException e) {
			mLogger.e("clearCache exception", e);
		} catch (IOException e) {
			mLogger.e("clearCache could not recreate the cache folder", e);
		}
	}

	private CacheItem getCacheItem(String UUID, int Height, boolean widthBased, boolean rounded) {
		CacheKey key = CacheKey.newUUIDBasedKey(UUID, Height, widthBased, EXT_MODE_AUTO, rounded?"_r":null);
		return getMap().get(key);
	}

	protected String getCachePath(String UUID, int height, boolean widthBased, boolean rounded) {
		mDataLock.lock();
		try {
			CacheItem cacheItem = getCacheItem(UUID, height, widthBased, rounded);
			if (cacheItem!=null) {
				File file = cacheItem.path;
				if (file!=null && file.exists())
					return file.getAbsolutePath();
			}
		} finally {
			mDataLock.unlock();
		}
		return null;
	}

	private boolean moveCachedFiles(CacheKey srcKey, CacheKey dstKey, int cacheType) {
		mLogger.v("Copy "+srcKey+" to "+dstKey);
		if (getMap().containsKey(dstKey)) {
			mLogger.d("item "+dstKey+" already exists in the DB");
			return false;
		}

		try {
			CacheItem v = getMap().get(srcKey);
			if (v!=null) {
				File src = v.path;
				if (src!=null && src.exists()) {
					File dst = getCachedFilepath(dstKey);
					dst.delete();

					if (src.renameTo(dst)) {
						v = v.copyWithNewPath(dst);
						v.type = cacheType;
						return put(dstKey, v)!=null;
					} else {
						mLogger.e("Failed to rename path "+src+" to "+dst);
					}
					//else mLogger.d(false, "keep the old version of "+newKey);
				}
			}
		} catch (Throwable e) {
			mLogger.e("failed to copy " + srcKey + " to " + dstKey, e);
		}
		return false;
	}

	protected void setCacheSize(int longTermSize, int shortTermSize, int eternalSize) {
		mCacheSizeLongterm = longTermSize;
		mCacheSizeEternal = eternalSize;
		mCacheSizeShortterm = shortTermSize;
		mLogger.d(this + " New cache size:" + mCacheSizeLongterm + " / " + mCacheSizeShortterm + " / " + mCacheSizeEternal);
		scheduleCustomOperation(new RemoveExpired());
	}

	@Override
	public void onNewBitmapLoaded(HashMap<CacheVariant,Bitmap> newBitmaps, String url, long remoteDate, final int type) {
		// handle the storing and adding to the cache
		// save the bitmap for later use
		long fileSizeAdded = 0;
		for (CacheVariant variant : newBitmaps.keySet()) {
			try {
				if (variant.path.exists())
					variant.path.delete();
				FileOutputStream fos = new FileOutputStream(variant.path, false);
				Bitmap bmp = newBitmaps.get(variant);
				bmp.compress(variant.key.getCompression(), variant.key.getCompRatio(), fos);
				fos.close();

				mDataLock.lock();
				try {
					CacheItem val = getMap().get(variant.key);
					if (val!=null) {
						if (val.remoteDate < remoteDate)
							val.remoteDate = remoteDate;

						if (CacheType.isStrictlyLowerThan(val.type, type))
							val.type = type;

						val.lastAccessDate = System.currentTimeMillis();
						notifyItemChanged(variant.key);
						/*if (!changed && url.equals(val.URL))
							mLogger.v("image " + key.toString()+" unchanged");
						else
							mLogger.v("image " + key.toString()+" already exists, adjusting the touitDate:"+val.touitID+" bmpIsNew:"+bmpIsNew+" rbmpIsNew:"+rbmpIsNew+" url:"+url);*/
					} else {
						val = new CacheItem(variant.path, url);
						val.remoteDate = remoteDate;
						val.type = type;
						val.lastAccessDate = System.currentTimeMillis();
						//mLogger.v("adding image " + key.toString() +" type:"+type+" bmpIsNew:"+bmpIsNew+" rbmpIsNew:"+rbmpIsNew+" url:"+url);
						put(variant.key, val);
					}

					fileSizeAdded += variant.path.length();
				} finally {
					mDataLock.unlock();
				}

				//mLogger.i("saved bmp to "+outFile.getAbsolutePath());
			} catch (IOException e) {
				mLogger.e("failed to save "+url+" as "+variant, e);
			}
		}

		//mLogger.i("BitmapLoaded outFile:"+outFile);
		if (fileSizeAdded!=0) {
			final boolean needsPurge;
			if (type==CacheType.CACHE_LONGTERM)
				needsPurge = (mPurgeCounterLongterm.incrementAndGet() > MIN_ADD_BEFORE_PURGE);
			else if (type == CacheType.CACHE_SHORTTERM)
				needsPurge = (mPurgeCounterShortterm.incrementAndGet() > MIN_ADD_BEFORE_PURGE);
			else
				needsPurge = false;

			if (needsPurge) {
				if (type==CacheType.CACHE_LONGTERM)
					mPurgeCounterLongterm.set(0);
				else if (type==CacheType.CACHE_SHORTTERM)
					mPurgeCounterShortterm.set(0);
				scheduleCustomOperation(new RemoveExpired(type));
			}
		}
	}


	File getCachedFile(CacheKey key, String URL, long itemDate) {
		//if (URL!=null && !URL.contains("/profile_images/"))
		//mLogger.v(" getPicture URL:"+URL + " key:"+key);
		if (key != null) {
			mDataLock.lock();
			try {
				CacheItem v = getMap().get(key);

				//if (URL!=null && !URL.contains("/profile_images/"))
				//mLogger.v(" found cache item "+v);
				if (v!=null) {
					try {
						if (URL!=null && !URL.equals(v.URL)) {
							// the URL for the cached item changed
							//mLogger.v(key+" changed from "+v.URL+" to "+URL+" v.touitID:"+v.touitDate +" touitDate:"+touitDate);
							if (v.remoteDate < itemDate) {
								// the item in the Cache is older than this request, the image changed for a newer one
								// we need to mark the old one as short term with a UUID that has the picture ID inside
								String dstUUID = getOldPicUUID(key.getUUID(), v.URL);
								CacheKey oldVersionKey = key.copyWithNewUuid(dstUUID);
								v = getMap().get(oldVersionKey);
								if (v==null)
									// the old version doesn't exist in the cache, copy the current content in there
									moveCachedFiles(key, oldVersionKey, CacheType.CACHE_SHORTTERM);
								remove(key); // this one is not valid anymore
							} else {
								// use the old image from the cache
								String dstUUID = getOldPicUUID(key.getUUID(), URL);
								key = key.copyWithNewUuid(dstUUID);
								v = getMap().get(key);
							}
						}

						// check if the URL matches, otherwise we have to load it again
						if (v!=null)
							return v.path;
					} catch (SecurityException e) {
						mLogger.e("getPicture exception:" + e.getMessage(), e);
					} catch (OutOfMemoryError e) {
						mLogger.w("Could not decode image " + URL, e);
						OOMHandler.handleOutOfMemory(mContext, postHandler, e);
					}
				}
				//else mLogger.i(key.toString()+" not found in "+mData.size()+" cache elements");
			} finally {
				mDataLock.unlock();
			}
		}
		return null;
	}

	public SimpleLogger getLogger() {
		return mLogger;
	}

	public Context getContext() {
		return mContext;
	}
}
