package org.gillius.jagnet;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionListenerChain implements ConnectionListener {
	private final List<ConnectionListener> listeners;

	public static ConnectionListenerChain of(ConnectionListener... listeners) {
		return new ConnectionListenerChain(Arrays.asList(listeners));
	}

	public ConnectionListenerChain() {
		this.listeners = new CopyOnWriteArrayList<>();
	}

	public ConnectionListenerChain(List<? extends ConnectionListener> listeners) {
		this.listeners = new CopyOnWriteArrayList<>(listeners);
	}

	public void addListener(ConnectionListener listener) {
		listeners.add(listener);
	}

	public void removeListener(ConnectionListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void onConnected(ConnectionListenerContext ctx) {
		ChainConnectionListenerContext chainContext = new ChainConnectionListenerContext(ctx);
		for (ConnectionListener listener : listeners) {
			listener.onConnected(chainContext);
			if (chainContext.consumed)
				return;
		}
	}

	@Override
	public void onDisconnected(ConnectionListenerContext ctx) {
		ChainConnectionListenerContext chainContext = new ChainConnectionListenerContext(ctx);
		for (ConnectionListener listener : listeners) {
			listener.onDisconnected(chainContext);
			if (chainContext.consumed)
				return;
		}
	}

	@Override
	public void onReceive(ConnectionListenerContext ctx, Object message) {
		ChainConnectionListenerContext chainContext = new ChainConnectionListenerContext(ctx);
		for (ConnectionListener listener : listeners) {
			listener.onReceive(chainContext, message);
			if (chainContext.consumed)
				return;
		}
	}

	private static class ChainConnectionListenerContext extends SingleConnectionListenerContext {
		public boolean consumed = false;

		public ChainConnectionListenerContext(ConnectionListenerContext ctx) {
			super(ctx.getConnection());
		}

		@Override
		public void consumeCurrentEvent() {
			consumed = true;
		}
	}
}
