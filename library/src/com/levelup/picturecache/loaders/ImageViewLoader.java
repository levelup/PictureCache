package com.levelup.picturecache.loaders;

import android.widget.ImageView;

import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

/**
 * Abstract class used to display the loaded/default/error bitmap in an ImageView
 * <p>
 * @see {@link ImageViewLoaderDefaultResource} and {@link ImageViewLoaderDefaultDrawable} 
 */
public abstract class ImageViewLoader extends ViewLoader<ImageView> {

	protected ImageViewLoader(ImageView view, StorageTransform storageTransform,
	                          BitmapTransform loadTransform) {
		super(view, storageTransform, loadTransform);
	}

}
