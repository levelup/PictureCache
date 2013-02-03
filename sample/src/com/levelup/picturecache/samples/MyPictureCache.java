package com.levelup.picturecache.samples;

import android.content.Context;

import com.levelup.picturecache.AbstractUIHandler;
import com.levelup.picturecache.LifeSpan;
import com.levelup.picturecache.PictureCache;

public class MyPictureCache extends PictureCache {

	private static final int CACHE_SIZE_SHORTTERM = 200000; // 200kb 
	private static final int CACHE_SIZE_LONGTERM  = 100000; // 100kb 
	private static final int CACHE_SIZE_ETERNAL   =  80000; //  80kb 
	
	public MyPictureCache(Context context, AbstractUIHandler postHandler) {
		super(context, postHandler, null, null);
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
	protected int getCacheMaxSize(LifeSpan lifeSpan) {
		switch (lifeSpan) {
		case ETERNAL: return CACHE_SIZE_ETERNAL;
		case LONGTERM: return CACHE_SIZE_LONGTERM;
		case SHORTTERM: return CACHE_SIZE_SHORTTERM;
		}
		return 0;
	}

}
