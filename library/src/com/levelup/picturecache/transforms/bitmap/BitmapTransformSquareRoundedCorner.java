package com.levelup.picturecache.transforms.bitmap;

import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.PictureCache;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;

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

			final Paint paint = new Paint();
			final RectF rectF = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());

			paint.setAntiAlias(true);
			paint.setColor(Color.BLACK);
			canvas.drawRoundRect(rectF, roundRadius, roundRadius, paint);

			paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
			canvas.drawBitmap(bitmap, 0, 0, paint);
			return output;
		} catch (Throwable e) {
			LogManager.getLogger().e(PictureCache.TAG, "getRoundedCornerBitmap exception", e);
		}

		return bitmap;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		return false;
	}
	
}
