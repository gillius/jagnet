package org.gillius.jagnet;

public interface ConnectionListenerContext {
	Connection getConnection();

	/**
	 * Stop the current event from propagating to further listeners.
	 */
	void consumeCurrentEvent();
}
