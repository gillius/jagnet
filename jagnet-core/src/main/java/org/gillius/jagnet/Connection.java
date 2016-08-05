package org.gillius.jagnet;

import java.util.concurrent.CompletableFuture;

public interface Connection extends AutoCloseable {
	void sendReliable(Object message);

	void sendFast(Object message);

	boolean isOpen();

	@Override
	void close();

	CompletableFuture<Connection> getCloseFuture();
}
