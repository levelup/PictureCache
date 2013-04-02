package com.levelup.picturecache.transforms.storage;

import com.levelup.picturecache.transforms.bitmap.BitmapTransform;

import android.graphics.Bitmap;

/** a {@link StorageTransform} that consists of running a {@link BitmapTransform} */
public class StorageTransformWithBitmapTransform<T extends BitmapTransform> implements StorageTransform {
	
	private final BitmapTransform transform;
	private final String variant;
	
	/**
	 * constructor 
	 * @param transform the {@link BitmapTransform} to apply to the Bitmap
	 * @param variantPostfix the extra String to use for the cache storage after the transformation is done
	 */
	public StorageTransformWithBitmapTransform(T transform, String variantPostfix) {
		this.transform = transform;
		this.variant = variantPostfix;
	}

	/**
	 * constructor with a postfix variant based on the {@link BitmapTransform}
	 * @param transform the {@link BitmapTransform} to apply to the Bitmap
	 */
	public StorageTransformWithBitmapTransform(T transform) {
		this.transform = transform;
		this.variant = transform.getVariant();
	}

	@Override
	public String getVariantPostfix() {
		return variant;
	}

	@Override
	public Bitmap transformBitmapForStorage(Bitmap bitmap) {
		return transform.transformBitmap(bitmap);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this) return true;
		if (!(o instanceof StorageTransformWithBitmapTransform)) return false;
		StorageTransformWithBitmapTransform<?> s = (StorageTransformWithBitmapTransform<?>) o;
		return variant.equals(s.variant) && transform.equals(s.transform);
	}
	
	@Override
	public int hashCode() {
		return variant.hashCode()*31 + transform.hashCode();
	}

}
