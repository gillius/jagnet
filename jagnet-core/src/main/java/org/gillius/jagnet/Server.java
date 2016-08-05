package org.gillius.jagnet;

public interface Server extends ConnectionSource, AutoCloseable {
	void setPort(int port);

	void registerMessages(Iterable<Class<?>> messageTypes);

	void registerMessages(Class<?>... messageTypes);

	@Override
	void close();
}
