package com.levelup.picturecache.transforms.storage;

import android.graphics.Bitmap;

/**
 * a transform that can be applied to the Bitmap before it is used for storage
 */
public interface StorageTransform {

	/**
	 * a string representing this transform for differentiation in the cache storage with the plain version of other transformations
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
