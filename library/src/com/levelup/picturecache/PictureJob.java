package com.levelup.picturecache;

import java.security.NoSuchAlgorithmException;

import android.text.TextUtils;

public class PictureJob {

	private String mURL;
	private String mUUID;
	private long mFreshDate;
	private LifeSpan mLifeSpan;
	private int mDimension;
	private boolean mWidthBased;
	private StorageType mExtensionMode = StorageType.AUTO;
	protected PictureLoaderHandler mHandler;

	public static class Builder {

		private String mURL;
		private String mUUID;
		private long mFreshDate;
		private LifeSpan mLifeSpan = LifeSpan.LONGTERM;
		private int mDimension;
		private boolean mWidthBased;
		private StorageType mExtensionMode = StorageType.AUTO;
		protected final PictureLoaderHandler mHandler;

		public Builder(PictureLoaderHandler handler) {
			mHandler = handler;
		}

		public Builder setURL(String URL) {
			mURL = URL;
			return this;
		}

		public Builder setUUID(String UUID) {
			mUUID = UUID;
			return this;
		}

		/**
		 * set for how long the item should remain in the cache
		 * 
		 * @return the {@link PictureJob} being created
		 */
		public Builder setLifeType(LifeSpan lifeSpan) {
			mLifeSpan = lifeSpan;
			return this;
		}

		public Builder setFreshDate(long date) {
			mFreshDate = date;
			return this;
		}

		public Builder setDimension(int dimension, boolean widthBased) {
			mDimension = dimension;
			mWidthBased = widthBased;
			return this;
		}

		public Builder setExtensionMode(StorageType extensionMode) {
			mExtensionMode = extensionMode;
			return this;
		}

		public PictureJob build() {
			PictureJob pictureJob = new PictureJob();
			pictureJob.mURL = mURL;
			pictureJob.mUUID = mUUID;
			pictureJob.mFreshDate = mFreshDate;
			pictureJob.mLifeSpan = mLifeSpan;
			pictureJob.mDimension = mDimension;
			pictureJob.mWidthBased = mWidthBased;
			pictureJob.mExtensionMode = mExtensionMode;
			pictureJob.mHandler = mHandler;
			return pictureJob;
		}
	}

	private CacheKey buildKey() throws NoSuchAlgorithmException {
		if (!TextUtils.isEmpty(mUUID))
			return CacheKey.newUUIDBasedKey(mUUID, mDimension, mWidthBased, mExtensionMode,
					mHandler.getStorageTransform() != null ? mHandler.getStorageTransform().getVariantPostfix() : null);

		if (!TextUtils.isEmpty(mURL))
			return CacheKey.newUrlBasedKey(mURL, mDimension, mWidthBased, mExtensionMode,
					mHandler.getStorageTransform() != null ? mHandler.getStorageTransform().getVariantPostfix() : null);

		return null;
	}

	/**
	 * Retrieve picture into cache
	 * 
	 * @param cache
	 * @throws NoSuchAlgorithmException
	 */
	public void startLoading(PictureCache cache) throws NoSuchAlgorithmException {
		CacheKey key = buildKey();
		if (key == null) {
			LogManager.logger.w(PictureCache.TAG, "could not generate a CacheKey for " + mUUID + " / " + mURL);
			return;
		}

		cache.getPicture(mURL, key, mFreshDate, mHandler, mLifeSpan);
	}
}