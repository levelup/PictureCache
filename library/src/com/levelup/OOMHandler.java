package com.levelup;

import android.content.Context;
import android.widget.Toast;

import com.levelup.picturecache.BuildConfig;
import com.levelup.picturecache.R;

public class OOMHandler {

	public static void handleOutOfMemory(final Context context, HandlerUIThread handler, final OutOfMemoryError e) {
		if (BuildConfig.DEBUG) {
			if (context!=null) {
				handler.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						if (e==null)
							Toast.makeText(context, R.string.oom_alert, Toast.LENGTH_SHORT).show();
						else
							Toast.makeText(context, R.string.oom_warning, Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}

}
