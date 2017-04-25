package org.gillius.jagnet;

/**
 * Defines a functional interface to receive a typed message.
 */
@FunctionalInterface
public interface ReceivedMessageListener<T> {
	void onReceive(ConnectionListenerContext ctx, T message);
}
