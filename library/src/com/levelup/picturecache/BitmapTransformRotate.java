package com.levelup.picturecache;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class BitmapTransformRotate implements BitmapTransform {
	private final float mRotation;

	public BitmapTransformRotate(float rotation) {
		this.mRotation = rotation;
	}

	@Override
	public Bitmap transformBitmap(Bitmap bitmap) {
		if (mRotation!=Float.valueOf(0)) {
			Matrix matrix = new Matrix();
			matrix.postRotate(mRotation);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		}
		return bitmap;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (!(o instanceof BitmapTransformRotate)) return false;
		BitmapTransformRotate l = (BitmapTransformRotate) o;
		return Float.compare(mRotation, l.mRotation)==0;
	}
	
	@Override
	public int hashCode() {
		return Float.floatToIntBits(mRotation);
	}

}
