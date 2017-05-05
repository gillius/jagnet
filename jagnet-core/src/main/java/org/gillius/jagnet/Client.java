package org.gillius.jagnet;

public interface Client extends ConnectionSource, AutoCloseable {
	void start();
}
