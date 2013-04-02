package com.levelup.picturecache.transforms.bitmap;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/** rotate the Bitmap with the rotation provided in the constructor */
public class BitmapTransformRotate implements BitmapTransform {
	private final float mRotation;

	/**
	 * constructor of a {@link BitmapTransform} that rotates the image according to the rotation provided
	 * @param rotation rotation to apply to the Bitmap
	 */
	public BitmapTransformRotate(float rotation) {
		this.mRotation = rotation;
	}

	@Override
	public Bitmap transformBitmap(Bitmap bitmap) {
		if (mRotation!=0.0f) {
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

	@Override
	public String getVariant() {
		return "rot_"+mRotation;
	}
}
