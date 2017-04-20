package org.gillius.jagnet;

import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface Connection extends AutoCloseable {
	void sendReliable(Object message);

	void sendFast(Object message);

	SocketAddress getLocalAddress();

	SocketAddress getRemoteAddress();

	boolean isOpen();

	/**
	 * Request that the connection shut down. This does not actually block until the connection is closed, use
	 * {@link #getCloseFuture()} to determine that.
	 */
	@Override
	void close();

	/**
	 * Returns a future completed when the connection closes.
	 */
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
