package com.levelup.picturecache;

import android.graphics.Bitmap;

public class StorageTransformSquareRoundedCorner implements StorageTransform {

	private static StorageTransformSquareRoundedCorner instance;
	
	public static synchronized StorageTransformSquareRoundedCorner getInstance() {
		if (instance==null)
			instance = new StorageTransformSquareRoundedCorner();
		return instance;
	}
	
	private StorageTransformSquareRoundedCorner() {}
	
	@Override
	public String getVariantPostfix() {
		return "_r";
	}

	@Override
	public Bitmap transformBitmapForStorage(Bitmap bitmap) {
		return BitmapTransformSquareRoundedCorner.getInstance().transformBitmap(bitmap);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		return false;
	}

}
