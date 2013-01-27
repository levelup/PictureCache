package com.levelup.picturecache.samples;

import com.levelup.picturecache.AbstractUIHandler;
import com.levelup.picturecache.CacheType;
import com.levelup.picturecache.ImageViewLoader;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends Activity implements AbstractUIHandler {
	
	private MyPictureCache mCache;
	private long uiThread;

	static final String levelupAvatarURL = "https://si0.twimg.com/profile_images/1584807085/LevelUp-Logo-Avatarv5.png"; // '_normal' / '_bigger' / ''
	static final String plumeAvatarURL = "https://si0.twimg.com/profile_images/2794557097/a073ce673f72cb8fc1aa189ad7f28fd6.png"; // '_normal' / '_bigger' / ''
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final int screenDpi = getResources().getDisplayMetrics().densityDpi;
		
		uiThread = Thread.currentThread().getId();
		mCache = new MyPictureCache(this, this);

		findViewById(R.id.loadAvatar1).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ImageView avatar = (ImageView) findViewById(R.id.avatar1);
				ImageViewLoader loader = new ImageViewLoader(avatar, null, null, null);
				mCache.loadPictureWithFixedHeight(loader, levelupAvatarURL, "twitter_levelup", 0,
						CacheType.CACHE_LONGTERM,
						(48*screenDpi)/160,
						MyPictureCache.EXT_MODE_AUTO);

			}
		});

		findViewById(R.id.loadAvatar2).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ImageView avatar = (ImageView) findViewById(R.id.avatar2);
				ImageViewLoaderDefaultResource loader = new ImageViewLoaderDefaultResource(avatar, R.drawable.picholder, null, null);
				mCache.loadPictureWithFixedHeight(loader, plumeAvatarURL, "twitter_plume", 0,
						CacheType.CACHE_LONGTERM,
						(96*screenDpi)/160,
						MyPictureCache.EXT_MODE_AUTO);

			}
		});

		findViewById(R.id.clearCache).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mCache.clear();
			}
		});
	}

	@Override
	public boolean isUIThread() {
		return uiThread == Thread.currentThread().getId();
	}
}
