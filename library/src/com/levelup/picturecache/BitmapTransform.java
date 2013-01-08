package com.levelup.picturecache;

import android.graphics.Bitmap;

public interface BitmapTransform {

	/**
	 * transform the source bitmap into the bitmap for display
	 * @param bitmap
	 * @return bitmap if no transformation is applied
	 */
	Bitmap transformBitmap(Bitmap bitmap);
}
