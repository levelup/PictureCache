package com.levelup.picturecache;

import java.io.File;

import android.graphics.Bitmap;


public abstract class PictureLoaderHandler {

	abstract protected void drawDefaultPicture(String url, AbstractUIHandler postHandler);
	abstract protected void drawBitmap(Bitmap bmp, String url, AbstractUIHandler postHandler);
	
	protected PictureLoaderHandler(StorageTransform bitmapStorageTransform, BitmapTransform bitmapTransform) {
		this.mStorageTransform = bitmapStorageTransform;
		this.mBitmapTransform = bitmapTransform;
	}
	
	protected StorageTransform getStorageTransform() {
		return mStorageTransform;
	}
	
	protected BitmapTransform getDisplayTransform() {
		return mBitmapTransform;
	}
	
	/**
	 * 
	 * @param downloadManager
	 * @param newURL
	 * @return true if the URL is new for this target
	 */
	abstract protected boolean setLoadingNewURL(DownloadManager downloadManager, String newURL);
	/**
	 * 
	 * @return
	 */
	abstract protected String getLoadingURL();
	abstract protected boolean canDirectLoad(File file, AbstractUIHandler uiHandler);
	
	protected boolean isDownloadAllowed() {
		return true;
	}

	protected final BitmapTransform mBitmapTransform;
	protected final StorageTransform mStorageTransform;

	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (!(o instanceof PictureLoaderHandler)) return false;
		PictureLoaderHandler loader = (PictureLoaderHandler) o;
		return (mBitmapTransform==null && loader.mBitmapTransform==null) || (mBitmapTransform!=null && mBitmapTransform.equals(loader))
				&& (mStorageTransform==null && loader.mStorageTransform==null) || (mStorageTransform!=null && mStorageTransform.equals(loader));
	}
	
	@Override
	public int hashCode() {
		return (mBitmapTransform==null ? 0 : mBitmapTransform.hashCode()) * 31 + (mStorageTransform==null ? 0 : mStorageTransform.hashCode());
	}
}