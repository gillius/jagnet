package org.gillius.jagnet;

import java.util.concurrent.CompletableFuture;

public interface ConnectionSource {
	void start();

	CompletableFuture<Connection> getConnection();
}
