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
	 * <p>This method may be called outside of the UI thread</p>
	 * 
	 * @param url URL being loaded
	 * @param drawableCache TODO
	 */
	public abstract void drawDefaultPicture(String url, BitmapLruCache drawableCache);
	
	/**
	 * Called when the download failed and an error should be displayed
	 * <p>This method may be called outside of the UI thread</p>
	 * 
	 * @param url URL being loaded
	 * @param drawableCache TODO
	 */
	public abstract void drawErrorPicture(String url, BitmapLruCache drawableCache);
	
	/**
	 * Called when the downloaded {@link Bitmap} should be displayed 
	 * <p>This method may be called outside of the UI thread</p>
	 * 
	 * @param drawable Drawable to display
	 * @param url URL corresponding to the bitmap
	 * @param cookie Data associated with the loaded URL, see {@link PictureJob.Builder#setCookie(Object)}
	 * @param drawableCache The bitmap memory cache in case you need to do things with the Drawable
	 * @param immediate {@code true} if the drawable should be displayed as soon as possible in the UI thread
	 */
	public abstract void drawBitmap(Drawable drawable, String url, Object cookie, BitmapLruCache drawableCache, boolean immediate);
	
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
	
	/**
	 * Allow downloading pictures in the calling thread (usually the UI thread), not recommended, won't happen in the UI thread in the memory cache
	 * @param file File that we are trying to decode in the calling thread
	 * @return {@code true} if the file can be decoded
	 */
	abstract protected boolean canDirectLoad(File file);

	/**
	 * Callback to check if we want to disable all downloads
	 * @return {@code false} if you don't want any downloads to happen
	 */
	public boolean isDownloadAllowed() {
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