package org.gillius.jagnet;

public interface ConnectionListener {
	default void onConnected(ConnectionListenerContext ctx) {}

	default void onDisconnected(ConnectionListenerContext ctx) {}

	default void onReceive(ConnectionListenerContext ctx, Object message) {}
}
