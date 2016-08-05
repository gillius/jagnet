package org.gillius.jagnet;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DeferredConnectionListener extends DelegatingConnectionListener implements Runnable {
	private ConcurrentLinkedQueue<Runnable> deferred = new ConcurrentLinkedQueue<>();

	public DeferredConnectionListener(ConnectionListener delegate) {
		super(delegate);
	}

	@Override
	public void onConnected(Connection connection) {
		deferred.add( () -> super.onConnected(connection));
	}

	@Override
	public void onDisconnected(Connection connection) {
		deferred.add( () -> super.onDisconnected(connection));
	}

	@Override
	public void onReceive(Connection connection, Object message) {
		deferred.add( () -> super.onReceive(connection, message));
	}

	@Override
	public void run() {
		Runnable r = deferred.poll();
		while (r != null) {
			r.run();
			r = deferred.poll();
		}
	}
}
