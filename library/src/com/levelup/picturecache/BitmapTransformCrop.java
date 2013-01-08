package com.levelup.picturecache;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;

public class BitmapTransformCrop implements BitmapTransform {

	private final int width;
	private final int height;
	
	public BitmapTransformCrop(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	@Override
	public Bitmap transformBitmap(Bitmap bitmap) {
		Paint paint = new Paint();
		paint.setFilterBitmap(true);

		final Bitmap clippedSrc;
		final float scale;
		if (bitmap.getWidth()*height < bitmap.getHeight()*width) {
			// we need to cut an horizontal band in the source
			scale = ((float) width) / bitmap.getWidth();
			//Log.wtf("PlumeCache", "we need to cut an horizontal band in the source "+bitmap.getWidth()+"x"+bitmap.getHeight()+" into "+width+"x"+height+" scale:"+scale);
			Matrix m = new Matrix();
			if (Float.compare(scale, (float) 1.0)!=0) m.postScale(scale, scale);
			//clippedSrc = Bitmap.createBitmap(bitmap, 0, (bitmap.getHeight() - (int) (height*scale))/2, bitmap.getWidth(), (int) (height*scale), m, true);
			//Log.wtf("PlumeCache", " cut at "+(bitmap.getHeight() - height)/2);
			//clippedSrc = Bitmap.createBitmap(bitmap, 0, (bitmap.getHeight() - height)/2, bitmap.getWidth(), height, m, true);
			final int y = (bitmap.getHeight() - (int)(height/scale))/2;
			clippedSrc = Bitmap.createBitmap(bitmap, 0, y, bitmap.getWidth(), (int)(height/scale), m, true);
		} else {
			// we need to cut a vertical band in the source
			scale = ((float) height) / bitmap.getHeight();
			//Log.wtf("PlumeCache", "we need to cut a vertical band in the source "+bitmap.getWidth()+"x"+bitmap.getHeight()+" into "+width+"x"+height+" scale:"+scale);
			Matrix m = new Matrix();
			if (Float.compare(scale, (float) 1.0)!=0) m.postScale(scale, scale);
			//Log.wtf("PlumeCache", " cut at "+(bitmap.getWidth() - width)/2);				
			//clippedSrc = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - (int) (width*scale))/2, 0, (int) (width*scale), bitmap.getHeight(), m, true);
			//clippedSrc = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - width)/2, 0, (int) (width*scale), bitmap.getHeight(), m, true);
			final int x = (bitmap.getWidth() - (int)(width/scale))/2;
			clippedSrc = Bitmap.createBitmap(bitmap, x, 0, (int)(width/scale), bitmap.getHeight(), m, true);
		}
		//Log.wtf("PlumeCache", " clipped "+clippedSrc.getWidth()+"x"+clippedSrc.getHeight());
		return Bitmap.createScaledBitmap(clippedSrc, width, height, true);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (!(o instanceof BitmapTransformCrop)) return false;
		BitmapTransformCrop l = (BitmapTransformCrop) o;
		return width==l.width && height==l.height;
	}
	
	@Override
	public int hashCode() {
		return width * 31 + height;
	}
}
