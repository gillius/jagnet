package org.gillius.jagnet;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface Connection extends AutoCloseable {
	void sendReliable(Object message);

	void sendFast(Object message);

	boolean isOpen();

	@Override
	void close();

	CompletableFuture<Connection> getCloseFuture();

	/**
	 * Submits a task to run on this connection's event loop -- such a task will not run concurrently with the
	 * {@link ConnectionListener}.
	 */
	void execute(Runnable task);
	/**
	 * Submits a task to run on this connection's event loop -- such a task will not run concurrently with the
	 * {@link ConnectionListener}.
	 */
	<T> Future<T> submit(Callable<T> task);
}
