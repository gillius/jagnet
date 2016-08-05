package org.gillius.jagnet.kryonet;

import com.esotericsoftware.kryonet.Listener;
import org.gillius.jagnet.Connection;
import org.gillius.jagnet.ConnectionListener;

public class KryonetClientConnectionListenerAdapter extends Listener {
	private final ConnectionListener listener;
	private final Connection connection;

	public KryonetClientConnectionListenerAdapter(ConnectionListener listener, Connection connection) {
		this.listener = listener;
		this.connection = connection;
	}

	@Override
	public void connected(com.esotericsoftware.kryonet.Connection kryoConnection) {
		listener.onConnected(connection);
	}

	@Override
	public void disconnected(com.esotericsoftware.kryonet.Connection kryoConnection) {
		listener.onDisconnected(connection);
	}

	@Override
	public void received(com.esotericsoftware.kryonet.Connection kryoConnection, Object object) {
		listener.onReceive(connection, object);
	}
}
