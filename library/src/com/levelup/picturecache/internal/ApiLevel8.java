package com.levelup.picturecache.internal;

import java.io.File;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Environment;

@TargetApi(8)
public class ApiLevel8 {

	public static File getPublicPictureDir() {
		return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
	}
	
	public static File getPrivatePictureDir(Context context) {
		return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
	}
	
}
