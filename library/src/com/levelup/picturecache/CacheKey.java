/**
 * 
 */
package com.levelup.picturecache;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.graphics.Bitmap;
import android.text.TextUtils;


class CacheKey {

	private final String UUID;          // key: the unique ID representing this item in the DB
	private final int dimension;        // key: the target display height/width
	private final boolean widthBased;   // key: whether it's width constrained or height constrained
	private final StorageType extensionMode;
	private final String variantString;
	private final int hashCode; // only compute the hascode once for speed efficiency

	private static class UseUrlBasedConstructor extends RuntimeException {private static final long serialVersionUID = -2231632339742517427L;}

	static CacheKey newUUIDBasedKey(String uuid, int height, boolean widthBased, StorageType extensionMode, String variantString) throws UseUrlBasedConstructor {
		if (TextUtils.isEmpty(uuid))
			throw new UseUrlBasedConstructor();

		uuid = uuid.replace('/', '_').replace(':', '_').replace('\'', '_');
		return new CacheKey(uuid, height, widthBased, extensionMode, variantString);
	}

	static CacheKey newUrlBasedKey(String srcURL, int height, boolean widthBased, StorageType extensionMode, String variantString) throws NoSuchAlgorithmException {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] md5 = digest.digest(srcURL.getBytes());
			String uuid = String.format("%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x", md5[0], md5[1], md5[2], md5[3], 
					md5[4], md5[5], md5[6], md5[7], md5[8], md5[9], md5[10], md5[11], md5[12], md5[13], md5[14], md5[15]);

			return new CacheKey(uuid, height, widthBased, extensionMode, variantString);
		} catch (NoSuchAlgorithmException e) {
			throw new NoSuchAlgorithmException("Failed to get a MD5 for " + srcURL, e);
		}
	}

	CacheKey copyWithNewUuid(String newUUID) {
		if (null==newUUID) throw new IllegalArgumentException("use copyWithNewUrl() instead");
		String uuid = newUUID.replace('/', '_').replace(':', '_').replace('\'', '_');
		return new CacheKey(uuid, dimension, widthBased, extensionMode, variantString);
	}

	CacheKey copyWithNewUrl(String newURL) {
		try {
			return newUrlBasedKey(newURL, dimension, widthBased, extensionMode, variantString);
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	private CacheKey(String uuid, int height, boolean widthBased, StorageType extensionMode, String variantString) {
		this.UUID = uuid;
		this.dimension = height;
		this.widthBased = widthBased;
		this.extensionMode = extensionMode;
		this.variantString = variantString;
		this.hashCode = ((((widthBased ? 31 : 0) + (variantString==null ? 0 : variantString.hashCode())) * 31 + dimension) * 31 + UUID.hashCode());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof CacheKey)) return false;
		CacheKey k = (CacheKey) o;
		//TouiteurLog.v(this.toString() + " == " + k.toString() + ":" + (k.Height==Height && k.UUID.equals(UUID)));
		return k.dimension==dimension && k.widthBased==widthBased && UUID.equals(k.UUID) && (variantString==null && k.variantString==null || variantString!=null && variantString.equals(k.variantString));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hashCode;
	}

	int getBitmapHeight(int outWidth, int outHeight) {
		int newHeight;
		if (widthBased) {
			newHeight = (dimension * outHeight) / outWidth;
			if (newHeight > outHeight)
				newHeight = outHeight;
		} else
			newHeight = dimension;

		return newHeight;
	}

	final String getUUID() {
		return UUID;
	}

	final boolean isWidthBased() {
		return widthBased;
	}

	@Override
	public String toString() {
		final StringBuilder hashBuilder = new StringBuilder(UUID);
		if (widthBased)
			hashBuilder.append("w");
		hashBuilder.append("_").append(dimension);
		if (variantString!=null)
			hashBuilder.append(variantString);
		return hashBuilder.toString();
	}

	String getFilename() {
		final StringBuilder filename = new StringBuilder(UUID);
		filename.append("_").append(dimension);
		if (variantString!=null)
			filename.append(variantString);
		filename.append(".").append(getExtension());
		return filename.toString();
	}

	private boolean isJPEG() {
		if (extensionMode==StorageType.JPEG) return true;
		if (extensionMode==StorageType.PNG) return false;
		return (widthBased && dimension > 150);
	}

	private String getExtension() {
		return isJPEG() ? "jpg" : "png";
	}

	Bitmap.CompressFormat getCompression() {
		return isJPEG() ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG;
	}

	int getCompRatio() {
		return isJPEG() ? 92 : 100;
	}

	public static CacheKey unserialize(String string) {
		String[] parts = string.split(":");
		String uuid = parts[0];
		int dimension = Integer.valueOf(parts[1]);
		boolean widthBased = parts[2].equals("w");
		StorageType extensionMode = StorageType.fromStorage(Integer.valueOf(parts[3]));
		String variantString = parts.length > 4 ? parts[4] : null;
		return newUUIDBasedKey(uuid, dimension, widthBased, extensionMode, variantString);
	}

	public String serialize() {
		return UUID+":"+dimension+":"+(widthBased?"w":"h")+":"+extensionMode.toStorage()+":"+(variantString==null?"":variantString);
	}
}