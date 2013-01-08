package com.levelup.picturecache;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;

public class BitmapTransformSquareRoundedCorner implements BitmapTransform {

	private static BitmapTransformSquareRoundedCorner instance;
	
	public static synchronized BitmapTransformSquareRoundedCorner getInstance() {
		if (instance==null)
			instance = new BitmapTransformSquareRoundedCorner();
		return instance;
	}
	
	private BitmapTransformSquareRoundedCorner() {}
	
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

		return getRoundedCornerBitmap(bitmap);
	}
	
	private static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
		// Get the pictures info
		//mLogger.d( "Bitmap size: w: " + bitmap.getWidth() + " h: " + bitmap.getHeight() );

		try {
			Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
			final Canvas canvas = new Canvas(output);

			final Paint paint = new Paint();
			final RectF rectF = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
			final float roundPx = 4;

			paint.setAntiAlias(true);
			paint.setColor(Color.BLACK);
			canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

			paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
			canvas.drawBitmap(bitmap, 0, 0, paint);
			return output;
		} catch (Throwable e) {
			//mLogger.e( "getRoundedCornerBitmap exception", e);
		}

		return bitmap;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		return false;
	}
	
}
