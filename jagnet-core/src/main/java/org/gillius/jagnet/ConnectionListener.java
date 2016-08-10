package org.gillius.jagnet;

public interface ConnectionListener {
	default void onConnected(Connection connection) {}

	default void onDisconnected(Connection connection) {}

	default void onReceive(Connection connection, Object message) {}
}
