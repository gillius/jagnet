package org.gillius.jagnet;

public interface Client extends ConnectionSource, AutoCloseable {
	void setPort(int port);

	void registerMessages(Iterable<Class<?>> messageTypes);

	void registerMessages(Class<?>... messageTypes);

	void setHost(String host);
}
