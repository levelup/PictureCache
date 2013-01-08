package com.levelup.log;

public interface AbstractLogger {
	void v(String msg) ;
	
	void d(String msg);
	
	void d(String msg, Throwable e);

	void i(String msg);
	
	void i(String msg, Throwable e);
	
	void w(String msg);
	
	void w(String msg, Throwable e);
	
	void e(String msg);
	
	void e(String msg, Throwable e);
}
