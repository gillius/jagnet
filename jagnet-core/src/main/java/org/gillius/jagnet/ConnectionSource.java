package org.gillius.jagnet;

import java.util.concurrent.CompletableFuture;

public interface ConnectionSource {
	CompletableFuture<Connection> getConnection();
}
