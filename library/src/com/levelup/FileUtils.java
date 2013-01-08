package com.levelup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import com.levelup.log.AbstractLogger;

public class FileUtils {

	public static void copyFile(File src, File dst, AbstractLogger logger) throws IOException {
		if (!src.exists()) {
			logger.w("source of copy "+dst.getAbsolutePath()+" doesn't exist");
			return;
		}
	
		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = new FileOutputStream(dst, false).getChannel();
	
		try {
			if (inChannel.transferTo(0, inChannel.size(), outChannel)==0)
				logger.e("nothing copied in "+dst.getAbsolutePath());
			//else logger.v("done copying to "+dst.getAbsolutePath());
		} finally {
			inChannel.close();
			outChannel.close();
		}
	}
}
