package org.gillius.jagnet;

/**
 * Exception thrown when a connection is disconnected.
 */
public class DisconnectException extends Exception {
	public DisconnectException() {
	}

	public DisconnectException(String message) {
		super(message);
	}

	public DisconnectException(String message, Throwable cause) {
		super(message, cause);
	}

	public DisconnectException(Throwable cause) {
		super(cause);
	}
}
