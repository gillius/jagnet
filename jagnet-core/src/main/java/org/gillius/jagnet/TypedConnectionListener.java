package org.gillius.jagnet;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A ConnectionListener that delegates {@link #onReceive(ConnectionListenerContext, Object)} events to different
 * listeners, depending on the exact type of the message object. If the registered listener is a
 * {@link ConnectionListener}, it also receives all connected and disconnected events.
 */
public class TypedConnectionListener implements ConnectionListener {
	private final Map<Class, ReceivedMessageListener> listeners = new IdentityHashMap<>();

	@Override
	public void onConnected(ConnectionListenerContext ctx) {
		getConnectionListenerStream()
		         .forEach(x -> x.onConnected(ctx));
	}

	@Override
	public void onDisconnected(ConnectionListenerContext ctx) {
		getConnectionListenerStream()
		         .forEach(x -> x.onDisconnected(ctx));
	}

	private Stream<ConnectionListener> getConnectionListenerStream() {
		return listeners.values().stream()
		                .filter(x -> x instanceof ConnectionListener)
		                .map(x -> (ConnectionListener) x);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onReceive(ConnectionListenerContext ctx, Object message) {
		ReceivedMessageListener listener = listeners.get(message.getClass());
		if (listener != null)
			listener.onReceive(ctx, message);
	}

	/**
	 * Sets (replaces) the listener for the given exact type with the given listener. If the listener is a
	 * {@link ConnectionListener}, the listener will also receive any connect and disconnect events.
	 *
	 * @return this object, useful for chaining multiple setListener calls.
	 */
	public <T> TypedConnectionListener setListener(Class<? super T> clazz, ReceivedMessageListener<T> listener) {
		listeners.put(clazz, listener);
		return this;
	}

	/**
	 * Removes any configured listener for the given type.
	 */
	public void removeListener(Class clazz) {
		listeners.remove(clazz);
	}
}
