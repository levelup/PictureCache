package com.levelup.picturecache.transforms.bitmap;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;

import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.PictureCache;

/**
 * create a square version with rounded corner of the Bitmap to transform
 * <p>
 * the Bitmap is cropped of its bigger dimension to create the square
 */
public class BitmapTransformSquareRoundedCorner implements BitmapTransform {

	private final int roundRadius;
	
	/** default constructor with a radius of 4 */
	public BitmapTransformSquareRoundedCorner() {
		this(4);
	}
	
	/**
	 * constructor of the {@link BitmapTransform} with the specified radius for the rounded corner
	 * @param roundRadius
	 */
	public BitmapTransformSquareRoundedCorner(int roundRadius) {
		this.roundRadius = roundRadius;
	}
	
	@Override
	public Bitmap transformBitmap(Bitmap bitmap) {
		if (bitmap == null)
			return null;

		if (bitmap.getHeight() != bitmap.getWidth()) {
			// make a cropped square version
			int size;
			if (bitmap.getHeight() < bitmap.getWidth())
				size = bitmap.getHeight(); 
			else
				size = bitmap.getWidth();

			int x = (bitmap.getWidth()/2) - (size/2);
			int y = (bitmap.getHeight()/2) - (size/2);

			bitmap = Bitmap.createBitmap(bitmap, x, y, size, size);
		}

		return getRoundedCornerBitmap(bitmap, roundRadius);
	}
	
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundRadius) {
		// Get the pictures info
		//mLogger.d( "Bitmap size: w: " + bitmap.getWidth() + " h: " + bitmap.getHeight() );

		try {
			Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
			final Canvas canvas = new Canvas(output);

			final BitmapShader shader;
			shader = new BitmapShader(bitmap, TileMode.CLAMP, TileMode.CLAMP);

			final Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setShader(shader);

			final RectF rect = new RectF(0.0f, 0.0f, bitmap.getWidth(), bitmap.getHeight());

			// rect contains the bounds of the shape
			// radius is the radius in pixels of the rounded corners
			// paint contains the shader that will texture the shape
			canvas.drawRoundRect(rect, roundRadius, roundRadius, paint);
			return output;
		} catch (Throwable e) {
			LogManager.getLogger().e(PictureCache.LOG_TAG, "getRoundedCornerBitmap exception", e);
		}

		return bitmap;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		return false;
	}
	
	@Override
	public String getVariant() {
		return "_r"+roundRadius;
	}
}
