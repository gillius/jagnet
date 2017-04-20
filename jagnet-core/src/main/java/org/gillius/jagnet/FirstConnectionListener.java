package org.gillius.jagnet;

import java.util.concurrent.CompletableFuture;

/**
 * Listens for the first connection and completes the future returned by {@link #getConnection()} when connection
 * completes.
 *
 * @see AcceptFirstPolicy
 */
public class FirstConnectionListener implements ConnectionStateListener, ConnectionSource {
	private final CompletableFuture<Connection> connectionFuture = new CompletableFuture<>();

	@Override
	public void onConnected(ConnectionListenerContext ctx) {
		connectionFuture.complete(ctx.getConnection());
	}

	@Override
	public CompletableFuture<Connection> getConnection() {
		return connectionFuture;
	}
}
