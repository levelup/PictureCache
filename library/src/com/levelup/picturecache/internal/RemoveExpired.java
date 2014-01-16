package com.levelup.picturecache.internal;

import java.io.File;
import java.util.Map.Entry;

import st.gaw.db.AsynchronousDbHelper;
import st.gaw.db.AsynchronousDbOperation;
import st.gaw.db.MapEntry;

import com.levelup.picturecache.LifeSpan;
import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.PictureCache;

public class RemoveExpired implements AsynchronousDbOperation<MapEntry<CacheKey,CacheItem>> {

	private final LifeSpan lifeSpan;

	public RemoveExpired() {
		this.lifeSpan = null;
	}

	public RemoveExpired(LifeSpan cacheType) {
		this.lifeSpan = cacheType;
	}

	@Override
	public void runInMemoryDbOperation(AsynchronousDbHelper<MapEntry<CacheKey, CacheItem>> db) {
		PictureCache cache = (PictureCache) db;
		if (lifeSpan != null)
			makeRoom(cache, lifeSpan);
		else {
			for (LifeSpan lifeSpan : LifeSpan.values())
				makeRoom(cache, lifeSpan);
		}
	}

	private static void makeRoom(PictureCache cache, LifeSpan lifeSpan) {
		if (PictureCache.DEBUG_CACHE) LogManager.getLogger().i(PictureCache.LOG_TAG, "start makeRoom for "+lifeSpan);
		try {
			long TotalSize = cache.getCacheSize(lifeSpan);
			int MaxSize = cache.getCacheMaxSize(lifeSpan);
			if (MaxSize != 0 && TotalSize > MaxSize) {
				// make room in the DB/cache for this new element
				while (TotalSize > MaxSize) {
					//if (type != k.getValue().type) continue;
					//long deleted = 0;
					Entry<CacheKey, CacheItem> entry = cache.getCacheOldestEntry(lifeSpan);
					if (entry == null)
						break;

					if (PictureCache.DEBUG_CACHE) LogManager.getLogger().i(PictureCache.LOG_TAG, "remove "+entry+" from the cache for "+lifeSpan);
					CacheItem item = cache.remove(entry.getKey());
					if (item != null) {
						File f = item.path;
						if (f != null && f.exists()) {
							long fSize = f.length();
							if (f.delete()) {
								TotalSize -= fSize;
								//deleted += fSize;
							}
						}
					}
					//LogManager.logger.d(TAG, "makeroom");
				}
			}
		} catch (NullPointerException e) {
			LogManager.getLogger().w(PictureCache.LOG_TAG, "can't make room for type:"+lifeSpan,e);
		}
		if (PictureCache.DEBUG_CACHE) LogManager.getLogger().i(PictureCache.LOG_TAG, "finished makeRoom for "+lifeSpan);
	}
}