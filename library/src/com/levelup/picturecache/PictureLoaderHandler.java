package com.levelup.picturecache;

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

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
public abstract class PictureLoaderHandler {
	
	/**
	 * Called when the default drawable should be displayed, while the bitmap is loading
	 * <p>
	 * This method may be called outside of the UI thread</p>
	 * 
	 * @param url URL being loaded
	 * @param drawableCache TODO
	 */
	public abstract void drawDefaultPicture(String url, BitmapLruCache drawableCache);
	
	/**
	 * Called when the downloaded {@link Bitmap} should be displayed 
	 * <p>
	 * This method may be called outside of the UI thread</p>
	 * 
	 * @param bmp Drawable to display
	 * @param url URL corresponding to the bitmap
	 * @param cookie data associated with the loaded URL
	 * @param drawableCache TODO
	 */
	public abstract void drawBitmap(Drawable bmp, String url, Object cookie, BitmapLruCache drawableCache);
	
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
	 * called to tell the loader which URL is being loaded in the target
	 * @param newURL
	 * @param cache TODO
	 * @return the URL that was previously loading, null if there wasn't any
	 */
	abstract protected String setLoadingURL(String newURL, BitmapLruCache cache);
	/**
	 * 
	 * @return
	 */
	abstract protected String getLoadingURL();
	abstract protected boolean canDirectLoad(File file);

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

	/**
	 * Tell if the downloaded Bitmap can be kept in memory for later use (not recommended for large bitmaps)
	 * <p>By default only allow bitmaps smaller than 1MB in memory</p>
	 * <p>A {@link BitmapLruCache} must be provided in the {@link PictureCache} constructor</p>
	 * @param bitmap The bitmap that should be kept in memory
	 * @return
	 */
	public boolean canKeepBitmapInMemory(final Bitmap bitmap) {
		if (bitmap==null)
			return false;
		return bitmap.getRowBytes() * bitmap.getHeight() < PictureCache.MAXBITMAP_IN_MEMORY;
	}
}