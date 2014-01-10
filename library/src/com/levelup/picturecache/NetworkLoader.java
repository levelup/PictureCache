package com.levelup.picturecache;

import java.io.InputStream;

import com.levelup.picturecache.internal.DownloadFailureException;

public interface NetworkLoader {

	public class NetworkLoaderException extends DownloadFailureException {
		private static final long serialVersionUID = -6401494306891861403L;

		/**
		 * Constructs a new {@code NetworkLoaderException} with the current stack trace
		 * and the specified detail message.
		 *
		 * @param detailMessage
		 *            the detail message for this exception.
		 */
		public NetworkLoaderException(String detailMessage) {
			super(detailMessage);
		}

		/**
		 * Constructs a new {@code NetworkLoaderException} with the current stack trace,
		 * the specified detail message and the specified cause.
		 *
		 * @param detailMessage
		 *            the detail message for this exception.
		 * @param throwable
		 *            the cause of this exception.
		 */
		public NetworkLoaderException(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
		}

		/**
		 * Constructs a new {@code NetworkLoaderException} with the current stack trace,
		 * the specified detail message and the specified cause.
		 *
		 * @param throwable
		 *            the cause of this exception.
		 */
		public NetworkLoaderException(Throwable throwable) {
			super(throwable);
		}
	}

	/**
	 * Get an {@link java.io.InputStream InputStream} for the given {@code url}
	 * @param url URL of the content to load
	 * @return {@link java.io.InputStream InputStream} for the URL or {@code null} if you want to use the default loading mechanism
	 * @throws NetworkLoaderException
	 */
	InputStream loadURL(String url) throws NetworkLoaderException;

}
