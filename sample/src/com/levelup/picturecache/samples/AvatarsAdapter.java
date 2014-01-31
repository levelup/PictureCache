package com.levelup.picturecache.samples;

import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.levelup.picturecache.PictureJob;
import com.levelup.picturecache.loaders.ImageViewLoaderDefaultResource;

/**
 * basic adapter that displays a list of items and their avatar
 */
class AvatarsAdapter extends ArrayAdapter<SampleSource.Sample> {
	protected final MyPictureCache mCache;

	AvatarsAdapter(Context context) {
		super(context, R.layout.list_item_avatar, android.R.id.text1, SampleSource.getSamples());

		mCache = MyPictureCache.getInstance(context);
	}

	@Override
	public final View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		
		// set the avatar on the inflated view at the specified position in the list
		try {
			// basic loader into an image view with a default display as "picholder" while it's loading 
			ImageViewLoaderDefaultResource loader = new ImageViewLoaderDefaultResource((ImageView) view.findViewById(R.id.avatar), R.drawable.picholder, null, null);

			// prepare a basic picture job to load the avatar URL with a specific UUID
			PictureJob avatarJob = new PictureJob.Builder(loader, loader, loader)
			.setURL(getItem(position).picURL)
			.setUUID("avatar_" + getItem(position).name)
			.build();

			// run the job in the picture cache
			avatarJob.startLoading(mCache);
		} catch (NoSuchAlgorithmException e) {
			Log.d("ListView","failed to use the avatar URL "+getItem(position).picURL);
		}

		return view;
	}
}