package com.levelup.picturecache.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import com.levelup.picturecache.LogManager;

public class FileUtils {

	public static void copyFile(File src, File dst) throws IOException {
		copyFile(src, dst, "FileCopy");
	}

	public static void copyFile(File src, File dst, String logTag) throws IOException {
		if (!src.exists()) {
			LogManager.getLogger().w(logTag, "source of copy "+dst.getAbsolutePath()+" doesn't exist");
			return;
		}

		FileChannel inChannel = null;
		FileChannel outChannel = null;

		try {
			inChannel = new FileInputStream(src).getChannel();
			outChannel = new FileOutputStream(dst, false).getChannel();
			if (inChannel.transferTo(0, inChannel.size(), outChannel)==0)
				LogManager.getLogger().e(logTag, "nothing copied in "+dst.getAbsolutePath());
			//else logger.v("done copying to "+dst.getAbsolutePath());
		} finally {
			if (null!=inChannel)
				inChannel.close();
			if (null!=outChannel)
				outChannel.close();
		}
	}

	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			if (files!=null) {
				for(File file : files) {
					if (file.isDirectory())
						deleteDirectory(file);
					else
						file.delete();
				}
			}
			return path.delete();
		}
		return true;
	}
}
