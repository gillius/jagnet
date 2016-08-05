package org.gillius.jagnet.kryonet;

import com.esotericsoftware.kryonet.*;
import org.gillius.jagnet.*;

import java.util.IdentityHashMap;
import java.util.Map;

public class KryonetConnectionListenerAdapter extends Listener {
	private final ConnectionListener listener;
	private final Map<com.esotericsoftware.kryonet.Connection, org.gillius.jagnet.Connection> connectionAdapters = new IdentityHashMap<>();

	public KryonetConnectionListenerAdapter(ConnectionListener listener) {
		this.listener = listener;
	}

	@Override
	public void connected(com.esotericsoftware.kryonet.Connection connection) {
		if (!listener.acceptingConnection(connection.getRemoteAddressTCP()))
			connection.close();
		else {
			KryonetConnectionAdapter adapter = new KryonetConnectionAdapter(connection);
			connectionAdapters.put(connection, adapter);
			listener.onConnected(adapter);
		}
	}

	private org.gillius.jagnet.Connection getConnection(com.esotericsoftware.kryonet.Connection connection) {
		return connectionAdapters.get(connection);
	}

	@Override
	public void disconnected(com.esotericsoftware.kryonet.Connection connection) {
		listener.onDisconnected(getConnection(connection));
		connectionAdapters.remove(connection);
	}

	@Override
	public void received(com.esotericsoftware.kryonet.Connection connection, Object object) {
		listener.onReceive(getConnection(connection), object);
	}
}
