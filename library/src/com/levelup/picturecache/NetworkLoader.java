package com.levelup.picturecache;

import java.io.InputStream;

public interface NetworkLoader {

	InputStream loadURL(String url);

}
