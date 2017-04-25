package org.gillius.jagnet;

/**
 * Threading model for connection listeners: All calls to the listener are serialized into a single event loop, for a
 * particular server or client.
 */
public interface ConnectionListener<T> extends ConnectionStateListener, ReceivedMessageListener<T> {
	@Override
	default void onReceive(ConnectionListenerContext ctx, T message) {}
}
