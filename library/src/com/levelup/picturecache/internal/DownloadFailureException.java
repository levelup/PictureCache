package com.levelup.picturecache.internal;

public class DownloadFailureException extends RuntimeException {

	private static final long serialVersionUID = -2721774521801388650L;

	/**
	 * Constructs a new {@code DownloadFailureException} that includes the current stack
	 * trace.
	 */
	public DownloadFailureException() {
	}

	/**
	 * Constructs a new {@code DownloadFailureException} with the current stack trace
	 * and the specified detail message.
	 *
	 * @param detailMessage
	 *            the detail message for this exception.
	 */
	public DownloadFailureException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * Constructs a new {@code DownloadFailureException} with the current stack trace
	 * and the specified cause.
	 *
	 * @param throwable
	 *            the cause of this exception.
	 */
	public DownloadFailureException(Throwable throwable) {
		super(throwable);
	}

	/**
	 * Constructs a new {@code DownloadFailureException} with the current stack trace,
	 * the specified detail message and the specified cause.
	 *
	 * @param detailMessage
	 *            the detail message for this exception.
	 * @param throwable
	 *            the cause of this exception.
	 */
	public DownloadFailureException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
