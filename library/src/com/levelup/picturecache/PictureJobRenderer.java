package com.levelup.picturecache;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface PictureJobRenderer {

	/**
	 * Called when the downloaded {@link Bitmap} should be displayed 
	 * 
	 * @param drawable Drawable to display
	 * @param url URL corresponding to the loaded bitmap
	 * @param drawCookie Data associated with the loaded URL, see {@link PictureJob.Builder#setDrawCookie(Object)}
	 * @param drawableCache The bitmap memory cache in case you need to do things with the Drawable
	 * @param immediate {@code true} if the drawable should be displayed as soon as possible in the UI thread
	 */
	void drawBitmap(Drawable drawable, String url, Object drawCookie, ThreadSafeBitmapLruCache drawableCache, boolean immediate);

	/**
	 * Called when the default drawable should be displayed, while the bitmap is loading
	 * 
	 * @param url URL being loaded
	 * @param drawableCache The bitmap memory cache in case you need to do things with the Drawable
	 */
	void drawDefaultPicture(String url, ThreadSafeBitmapLruCache drawableCache);

	/**
	 * Called when the download failed and an error should be displayed
	 * 
	 * @param url URL that failed to load
	 * @param drawableCache The bitmap memory cache in case you need to do things with the Drawable
	 */
	void drawErrorPicture(String url, ThreadSafeBitmapLruCache drawableCache);
}