package com.levelup.picturecache.transforms.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.widget.ImageView;

/**
 * resize the downloaded Bitmap to match the DPI provided in the constructor
 * <p>
 * if the target of the download is an {@link ImageView} you may want to use
 * {@link ImageView#setImageMatrix(Matrix)} instead to avoid memory copies
 */
public class BitmapTransformMatchDPI implements BitmapTransform {

	private final int deviceDPI;

	/**
	 * constructor of a {@link BitmapTransform} that resizes the image to match the DPI provided
	 * @param deviceDPI the DPI to match, 160 means identical bitmap on output
	 */
	public BitmapTransformMatchDPI(int deviceDPI) {
		this.deviceDPI = deviceDPI;
	}

	/**
	 * constructor of a {@link BitmapTransform} that resizes the image to match the DPI of the {@link Context}
	 * @param context the context to get the DPI from
	 */
	public BitmapTransformMatchDPI(Context context) {
		this.deviceDPI = context.getResources().getDisplayMetrics().densityDpi;
	}

	@Override
	public Bitmap transformBitmap(Bitmap bitmap) {
		if (deviceDPI!=160) {
			Matrix matrix = new Matrix();
			matrix.setScale((float)deviceDPI / 160f, (float)deviceDPI / 160f);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		}
		return bitmap;
	}

}
