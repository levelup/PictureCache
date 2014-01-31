package com.levelup.picturecache;

import com.levelup.picturecache.loaders.PrecacheImageLoader;
import com.levelup.picturecache.loaders.RemoteViewLoader;
import com.levelup.picturecache.loaders.ViewLoader;
import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

/**
 * base class for the loader used by the picture cache
 * <p>
 * a {@link PictureLoaderHandler} handles the display of the loaded Bitmap and the placeholder Bitmap during loading
 * it also handles the Bitmaps transformations use for storage and/or display
 * <p>
 * @see {@link ViewLoader}, {@link RemoteViewLoader} or {@link PrecacheImageLoader}
 */
public abstract class PictureLoaderHandler implements IPictureLoaderRender, IPictureLoaderTransforms, IPictureLoadConcurrency {
	
	protected PictureLoaderHandler(StorageTransform bitmapStorageTransform, BitmapTransform bitmapTransform) {
		this.mStorageTransform = bitmapStorageTransform;
		this.mBitmapTransform = bitmapTransform;
	}
	
	public StorageTransform getStorageTransform() {
		return mStorageTransform;
	}
	
	public BitmapTransform getDisplayTransform() {
		return mBitmapTransform;
	}
	
	/**
	 * 
	 * @return
	 */
	abstract protected String getLoadingURL();
	
	/**
	 * Callback to check if we want to disable all downloads
	 * @return {@code false} if you don't want any downloads to happen
	 */
	public boolean isDownloadAllowed() {
		return true;
	}

	private final BitmapTransform mBitmapTransform;
	private final StorageTransform mStorageTransform;

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