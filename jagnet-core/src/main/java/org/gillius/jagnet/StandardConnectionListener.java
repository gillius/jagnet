package org.gillius.jagnet;

public class StandardConnectionListener extends DelegatingConnectionListener {
	public StandardConnectionListener() {
	}

	public StandardConnectionListener(ConnectionListener delegate) {
		super(delegate);
	}

	@Override
	public void onReceive(Connection connection, Object message) {
		if (message instanceof TimeSyncRequest) {
			TimeSync.receiveRequestAndRespond(connection, (TimeSyncRequest) message);
		} else {
			super.onReceive(connection, message);
		}
	}
}
