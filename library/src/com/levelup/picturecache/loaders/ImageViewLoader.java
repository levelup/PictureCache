package com.levelup.picturecache.loaders;

import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

import android.widget.ImageView;

/**
 * the base class used to display the loaded/default bitmap in an ImageView
 * <p>
 * @see {@link ImageViewLoaderDefaultResource} and {@link ImageViewLoaderDefaultDrawable} 
 */
public abstract class ImageViewLoader extends ViewLoader<ImageView> {

	public ImageViewLoader(ImageView view, StorageTransform storageTransform,
			BitmapTransform loadTransform) {
		super(view, storageTransform, loadTransform);
	}

}
