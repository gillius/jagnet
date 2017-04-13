package org.gillius.jagnet;

import java.util.function.Function;

public interface Server extends ConnectionSource, AutoCloseable {
	void setPort(int port);

	void registerMessages(Iterable<Class<?>> messageTypes);

	void registerMessages(Class<?>... messageTypes);

	/**
	 * A shortcut for calling {@link #setListenerFactory(Function)} with a factory always returning this listener.
	 */
	default void setListener(ConnectionListener listener) {
		setListenerFactory(x -> listener);
	}

	/**
	 * A function that given the incoming connection context, returns a {@link ConnectionListener} to use for that
	 * connection. Since a listener for a given connection is not called concurrently, this allows the function to return
	 * connection-specific listener instances if they are stateful. Or, the factory could return the same value each time
	 * if the listener is stateless or thread-safe.
	 * <p>
	 * This method must be called before the server is started and cannot be null.
	 */
	void setListenerFactory(Function<NewConnectionContext, ConnectionListener> factory);

	@Override
	void close();
}
