package com.levelup.picturecache.internal;

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;

public class ThreadSafeBitmapLruCache {

	private final BitmapLruCache cache;
	
	public ThreadSafeBitmapLruCache(BitmapLruCache cache) {
		this.cache = cache;
	}

	public synchronized CacheableBitmapDrawable get(String cacheKey) {
		return cache.get(cacheKey);
	}

	public synchronized CacheableBitmapDrawable put(String cacheKey, File file) {
		return cache.put(cacheKey, file);
	}

	public synchronized CacheableBitmapDrawable put(String cacheKey, File file, Options decodeOpts) {
		return cache.put(cacheKey, file, decodeOpts);
	}

	public synchronized CacheableBitmapDrawable put(String cacheKey, Bitmap bitmap) {
		return cache.put(cacheKey, bitmap);
	}
	
	public synchronized void trimMemory() {
		cache.trimMemory();
	}

}
