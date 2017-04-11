package org.gillius.jagnet;

/**
 * Threading model for connection listeners: All calls to the listener are serialized into a single event loop, for a
 * particular server or client.
 */
public interface ConnectionListener {
	default void onConnected(ConnectionListenerContext ctx) {}

	default void onDisconnected(ConnectionListenerContext ctx) {}

	default void onReceive(ConnectionListenerContext ctx, Object message) {}
}
