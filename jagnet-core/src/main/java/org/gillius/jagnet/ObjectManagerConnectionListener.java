package org.gillius.jagnet;

public class ObjectManagerConnectionListener implements ConnectionListener {
	private final ObjectManager manager;

	public ObjectManagerConnectionListener(ObjectManager manager) {
		this.manager = manager;
	}

	@Override
	public void onReceive(ConnectionListenerContext ctx, Object message) {
		if (manager.handleMessage(message))
			ctx.consumeCurrentEvent();
	}
}
