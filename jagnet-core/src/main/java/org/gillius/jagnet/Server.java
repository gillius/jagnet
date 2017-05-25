package org.gillius.jagnet;

public interface Server extends AutoCloseable {
	AcceptPolicy getAcceptPolicy();

	void setAcceptPolicy(AcceptPolicy acceptPolicy);

	ConnectionStateListener getConnectionStateListener();

	void setConnectionStateListener(ConnectionStateListener connectionStateListener);

	void start(ConnectionParams params);

	void stopAcceptingNewConnections();

	@Override
	void close();
}
