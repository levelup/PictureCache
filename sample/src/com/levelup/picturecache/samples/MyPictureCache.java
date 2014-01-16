package com.levelup.picturecache.samples;

import android.content.Context;

import com.levelup.picturecache.LifeSpan;
import com.levelup.picturecache.PictureCache;

public class MyPictureCache extends PictureCache {

	private static final int CACHE_SIZE_SHORTTERM = 800000; // 800kb
	private static final int CACHE_SIZE_LONGTERM  = 100000; // 100kb
	private static final int CACHE_SIZE_ETERNAL   =  80000; //  80kb

	private static MyPictureCache instance;
	
	static synchronized MyPictureCache getInstance(Context context) {
		if (instance == null)
			instance = new MyPictureCache(context);
		return instance;
	}
	
	private MyPictureCache(Context context) {
		super(context, null, null, 4*1024*1024);
	}

	@Override
	protected String getOldPicUUID(String uuid, String URL) {
		return null;
	}

	@Override
	protected String getAppName() {
		return getContext().getString(R.string.app_name);
	}

	@Override
	public int getCacheMaxSize(LifeSpan lifeSpan) {
		switch (lifeSpan) {
		case ETERNAL: return CACHE_SIZE_ETERNAL;
		case LONGTERM: return CACHE_SIZE_LONGTERM;
		case SHORTTERM: return CACHE_SIZE_SHORTTERM;
		}
		return 0;
	}

}
