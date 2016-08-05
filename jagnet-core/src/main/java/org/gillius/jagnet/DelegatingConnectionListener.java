package org.gillius.jagnet;

import java.net.InetSocketAddress;

public abstract class DelegatingConnectionListener implements ConnectionListener {
	protected final ConnectionListener delegate;

	public DelegatingConnectionListener() {
		this(null);
	}

	public DelegatingConnectionListener(ConnectionListener delegate) {
		if (delegate == null)
			delegate = NoopConnectionListener.INSTANCE;
		this.delegate = delegate;
	}

	@Override
	public boolean acceptingConnection(InetSocketAddress address) {
		return delegate.acceptingConnection(address);
	}

	@Override
	public void onConnected(Connection connection) {
		delegate.onConnected(connection);
	}

	@Override
	public void onDisconnected(Connection connection) {
		delegate.onDisconnected(connection);
	}

	@Override
	public void onReceive(Connection connection, Object message) {
		delegate.onReceive(connection, message);
	}
}
