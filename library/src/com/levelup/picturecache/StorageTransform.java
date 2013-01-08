package com.levelup.picturecache;

import android.graphics.Bitmap;

public interface StorageTransform {

	/**
	 * a string representing this transform for differentiation in the cache storage
	 * @return not null
	 */
	String getVariantPostfix();

	/**
	 * transform the source bitmap into the bitmap that will be stored in cache
	 * @param bitmap
	 * @return bitmap if no transformation is applied
	 */
	Bitmap transformBitmapForStorage(Bitmap bitmap);
}
