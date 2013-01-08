package com.levelup.picturecache;

import java.io.File;
import java.util.Random;

import com.levelup.HandlerUIThread;
import com.levelup.SimpleLogger;

import android.graphics.Bitmap;

public class PrecacheImageLoader extends PictureLoaderHandler {
	
	private final int mUniqueID;
	private static final Random RAND = new Random();
	private String mLoadingUrl;

	public PrecacheImageLoader(StorageTransform storageTransform, BitmapTransform loadTransform) {
		super(storageTransform, loadTransform);
		mUniqueID = RAND.nextInt();
	}

	@Override
	public void drawDefaultPicture(String url, HandlerUIThread postHandler, SimpleLogger logger) {}

	@Override
	public void drawBitmap(Bitmap bmp, String url, HandlerUIThread postHandler, SimpleLogger logger) {}

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
	public boolean setLoadingNewURL(DownloadManager downloadManager, String newURL, SimpleLogger logger) {
		if (newURL!=null && newURL.equals(mLoadingUrl))
			return false;
		downloadManager.cancelDownloadForLoader(this, mLoadingUrl);
		mLoadingUrl = newURL;
		return true;
	}

	@Override
	public String getLoadingURL() {
		return mLoadingUrl;
	}

	@Override
	protected boolean canDirectLoad(File file) {
		return true;
	}
}
