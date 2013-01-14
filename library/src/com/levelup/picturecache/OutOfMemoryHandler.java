package com.levelup.picturecache;

/**
 * interface to receive {@link OutOfMemoryError} exceptions from the library 
 */
public interface OutOfMemoryHandler {

	/**
	 * notify an {@link OutOfMemoryError} exception has been received
	 * @param e
	 */
	void onOutOfMemoryError(OutOfMemoryError e);
	
}
