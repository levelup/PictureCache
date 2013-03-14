package com.levelup.picturecache.loaders;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

/**
 * class used by the picture cache to display bitmaps on an {@link View}, the default placeholder is defined by a {@link Drawable}
 * <p>
 * it also handles the Bitmaps transformations use for storage and/or display
 */
public class ViewLoaderDefaultDrawable<T extends View> extends ViewLoader<T> {
	private final Drawable defaultDrawable;

	/**
	 * constructor of the {@link ViewLoaderDefaultDrawable}
	 * @param view the ImageView on which the loaded bitmap will be displayed
	 * @param defaultDrawable a drawable to be used as placeholder while the bitmap is loading, may be null
	 * @param storageTransform the transformation to use before storing the bitmap in the cache
	 * @param loadTransform the non-persistent transformation to use on the bitmap before displaying it
	 */
	public ViewLoaderDefaultDrawable(T view, Drawable defaultDrawable, StorageTransform storageTransform, BitmapTransform loadTransform) {
		super(view, storageTransform, loadTransform);
		this.defaultDrawable = defaultDrawable;
	}

	/**
	 * display the default view, called in the UI thread
	 * called under a lock on {@link view}
	 */
	protected void displayDefaultView() {
		if (getImageView() instanceof ImageView)
			((ImageView) getImageView()).setImageDrawable(defaultDrawable);
	}
}