package com.levelup.picturecache.transforms.bitmap;

import android.graphics.Bitmap;

/** a transform that can be applied to the Bitmap before it is displayed, the transformation is not used for storage in the cache */
public interface BitmapTransform {

	/**
	 * transform the source bitmap into the bitmap for display
	 * @param bitmap the Bitmap to transformed
	 * @return bitmap the transformed Bitmap, may be the same as the source if no transformation occured
	 */
	Bitmap transformBitmap(Bitmap bitmap);
}
