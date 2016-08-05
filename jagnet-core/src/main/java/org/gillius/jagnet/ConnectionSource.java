package org.gillius.jagnet;

import java.util.concurrent.CompletableFuture;

public interface ConnectionSource {
	void setListener(ConnectionListener listener);

	void start();

	CompletableFuture<Connection> getConnection();
}
