package org.gillius.jagnet;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DeferredConnectionListener implements ConnectionListener, Runnable {
	private final ConnectionListener delegate;
	private final ConcurrentLinkedQueue<Runnable> deferred = new ConcurrentLinkedQueue<>();

	public DeferredConnectionListener(ConnectionListener delegate) {
		this.delegate = delegate;
	}

	public DeferredConnectionListener(ConnectionListener... chain) {
		delegate = ConnectionListenerChain.of(chain);
	}

	@Override
	public void onConnected(ConnectionListenerContext ctx) {
		ConnectionListenerContext deferredContext = new SingleConnectionListenerContext(ctx.getConnection());
		deferred.add( () -> delegate.onConnected(deferredContext));
	}

	@Override
	public void onDisconnected(ConnectionListenerContext ctx) {
		ConnectionListenerContext deferredContext = new SingleConnectionListenerContext(ctx.getConnection());
		deferred.add( () -> delegate.onDisconnected(deferredContext));
	}

	@Override
	public void onReceive(ConnectionListenerContext ctx, Object message) {
		ConnectionListenerContext deferredContext = new SingleConnectionListenerContext(ctx.getConnection());
		deferred.add( () -> delegate.onReceive(deferredContext, message));
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
