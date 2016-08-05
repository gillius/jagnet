package org.gillius.jagnet;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class SinglePlayerConnectionListener extends DelegatingConnectionListener {
	private final CompletableFuture<Connection> playerAcceptedFuture;

	public SinglePlayerConnectionListener(ConnectionListener delegate) {
		this(null, delegate);
	}

	public SinglePlayerConnectionListener(CompletableFuture<Connection> playerAcceptedFuture, ConnectionListener delegate) {
		super(delegate);
		if (playerAcceptedFuture == null)
			playerAcceptedFuture = new CompletableFuture<>();
		this.playerAcceptedFuture = playerAcceptedFuture;
	}

	public CompletableFuture<Connection> getPlayerAcceptedFuture() {
		return playerAcceptedFuture;
	}

	@Override
	public boolean acceptingConnection(InetSocketAddress address) {
		return !playerAcceptedFuture.isDone(); //accept clients until one has totally made it through
	}

	@Override
	public void onConnected(Connection connection) {
		if (!playerAcceptedFuture.complete(connection))
			connection.close(); //reject any extra clients
	}
}
