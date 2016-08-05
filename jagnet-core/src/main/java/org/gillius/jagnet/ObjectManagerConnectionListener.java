package org.gillius.jagnet;

public class ObjectManagerConnectionListener extends DelegatingConnectionListener {
	private final ObjectManager manager;

	public ObjectManagerConnectionListener(ObjectManager manager) {
		this.manager = manager;
	}

	public ObjectManagerConnectionListener(ConnectionListener delegate, ObjectManager manager) {
		super(delegate);
		this.manager = manager;
	}

	@Override
	public void onReceive(Connection connection, Object message) {
		if (!manager.handleMessage(message))
			super.onReceive(connection, message);
	}
}
