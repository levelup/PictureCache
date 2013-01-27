package com.levelup.picturecache.transforms.storage;

import com.levelup.picturecache.transforms.bitmap.BitmapTransformSquareRoundedCorner;

/** a {@link StorageTransform} that makes a square rounded version of the Bitmap for storage */
public class StorageTransformSquareRoundedCorner extends StorageTransformWithBitmapTransform<BitmapTransformSquareRoundedCorner> {

	/** constructor with rounded corners of 4 pixels */
	public StorageTransformSquareRoundedCorner() {
		super(new BitmapTransformSquareRoundedCorner(4), "_r");
	}
	
	/**
	 *  constructor with the rounded corner radius set in the parameter
	 * 
	 * @param roundedRadius radius of the rounded corner
	 */
	public StorageTransformSquareRoundedCorner(int roundedRadius) {
		super(new BitmapTransformSquareRoundedCorner(roundedRadius), roundedRadius==4 ? "_r" : ("_r"+roundedRadius));
	}
}
