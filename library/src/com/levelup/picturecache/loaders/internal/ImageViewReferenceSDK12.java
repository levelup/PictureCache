package com.levelup.picturecache.loaders.internal;


import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

@TargetApi(12)
public class ImageViewReferenceSDK12<V extends ImageView> extends ViewReferenceSDK12<V> {

	public ImageViewReferenceSDK12(V view) {
		super(view);
	}

	@Override
	protected void displayDrawable(Drawable drawable) {
		/*if (drawable instanceof BitmapDrawable) {
			// trick so the DPI is handled correctly
			//Bitmap src = ((BitmapDrawable) drawable).getBitmap();
			drawable = new CacheableBitmapDrawable("", getImageView().getContext().getResources(), ((BitmapDrawable) drawable).getBitmap(), BitmapLruCache.RecyclePolicy.DISABLED);
			//Bitmap okay = ((BitmapDrawable) drawable).getBitmap();
			//LogManager.getLogger().e(PictureCache.LOG_TAG, "gonna use "+okay.getWidth()+"x"+okay.getHeight()+"/"+okay.getDensity()+" instead of "+src.getWidth()+"x"+src.getHeight()+"/"+src.getDensity());
		}*/

		getImageView().setImageDrawable(drawable);
	}

}
