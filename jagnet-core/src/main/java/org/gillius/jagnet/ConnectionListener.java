package org.gillius.jagnet;

import java.net.InetSocketAddress;

public interface ConnectionListener {
	/**
	 * Event when the server is about to accept a new connection from a client. Return true to accept the client, false to
	 * reject.
	 */
	default boolean acceptingConnection(InetSocketAddress address) {
		return true;
	}

	default void onConnected(Connection connection) {}

	default void onDisconnected(Connection connection) {}

	default void onReceive(Connection connection, Object message) {}
}
