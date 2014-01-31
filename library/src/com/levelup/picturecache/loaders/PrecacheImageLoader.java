package com.levelup.picturecache.loaders;

import java.io.File;
import java.util.Random;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.graphics.drawable.Drawable;

import com.levelup.picturecache.PictureLoaderHandler;
import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;


public class PrecacheImageLoader extends PictureLoaderHandler {
	
	private final int mUniqueID;
	private static final Random RAND = new Random();
	private String mLoadingUrl;

	public PrecacheImageLoader(StorageTransform storageTransform, BitmapTransform loadTransform) {
		super(storageTransform, loadTransform);
		mUniqueID = RAND.nextInt();
	}

	@Override
	public void drawDefaultPicture(String url, BitmapLruCache drawableCache) {
		// do nothing
	}

	@Override
	public void drawErrorPicture(String url, BitmapLruCache drawableCache) {
		// do nothing
	}

	@Override
	public void drawBitmap(Drawable bmp, String url, Object cookie, BitmapLruCache drawableCache, boolean immediate) {
		// do nothing
	}

	@Override
	public boolean equals(Object o) {
		if (o==this) return true;
		if (!(o instanceof PrecacheImageLoader)) return false;
		PrecacheImageLoader loader = (PrecacheImageLoader) o;
		return loader.mUniqueID == mUniqueID && super.equals(loader);
	}
	
	@Override
	public int hashCode() {
		return mUniqueID;
	}

	@Override
	public String setLoadingURL(String newURL, BitmapLruCache cache) {
		String oldLoadingUrl = mLoadingUrl;
		mLoadingUrl = newURL;
		return oldLoadingUrl;
	}

	@Override
	public String getLoadingURL() {
		return mLoadingUrl;
	}

	@Override
	public boolean canDirectLoad(File file) {
		return false;
	}
}
