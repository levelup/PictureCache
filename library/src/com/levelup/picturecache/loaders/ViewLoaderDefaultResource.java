package com.levelup.picturecache.loaders;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;


/**
 * Display bitmaps on an {@link View}, the default/error placeholder is defined by a resource ID
 * <p>It also handles the Bitmaps transformations use for storage and/or display</p>
 */
public class ViewLoaderDefaultResource<T extends View> extends ViewLoader<T> {
	private final int defaultDrawable;

	/**
	 * constructor of the {@link ViewLoaderDefaultResource}
	 * @param view the ImageView on which the loaded bitmap will be displayed
	 * @param defaultResourceId the resource ID of a drawable to be used as placeholder while the bitmap is loading, may be {@code android.R.color.transparent}
	 * @param storageTransform the transformation to use before storing the bitmap in the cache
	 * @param loadTransform the non-persistent transformation to use on the bitmap before displaying it
	 */
	public ViewLoaderDefaultResource(T view, int defaultResourceId, StorageTransform storageTransform, BitmapTransform loadTransform) {
		super(view, storageTransform, loadTransform);
		this.defaultDrawable = defaultResourceId;
	}

	@Override
	public void displayDefaultView(BitmapLruCache drawableCache) {
		if (getImageView() instanceof ImageView) {
			if (drawableCache!=null) {
				final String drawableName = "android.resource://" + defaultDrawable;
				CacheableBitmapDrawable src = drawableCache.get(drawableName);
				if (src != null) {
					((ImageView) getImageView()).setImageDrawable(src);
				} else {
					Drawable resDrawable = getImageView().getResources().getDrawable(defaultDrawable);
					((ImageView) getImageView()).setImageDrawable(resDrawable);
					if (resDrawable instanceof BitmapDrawable) {
						drawableCache.put(drawableName, ((BitmapDrawable) resDrawable).getBitmap());
					}
				}
			} else {
				((ImageView) getImageView()).setImageResource(defaultDrawable);
			}
		}
	}
}