package org.gillius.jagnet.kryonet;

import org.gillius.jagnet.Connection;

public class KryonetConnectionAdapter implements Connection {
	private final com.esotericsoftware.kryonet.Connection kryonetConnection;

	public KryonetConnectionAdapter(com.esotericsoftware.kryonet.Connection kryonetConnection) {
		this.kryonetConnection = kryonetConnection;
	}

	@Override
	public void sendReliable(Object message) {
		kryonetConnection.sendTCP(message);
	}

	@Override
	public void sendFast(Object message) {
		kryonetConnection.sendUDP(message);
	}

	@Override
	public void close() {
		kryonetConnection.close();
	}
}
