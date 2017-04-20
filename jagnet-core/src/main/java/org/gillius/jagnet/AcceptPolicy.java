package org.gillius.jagnet;

/**
 * Interface to specify whether or not a server accepts a given connection.
 */
public interface AcceptPolicy {
	/**
	 * Event when the server is about to accept a new connection from a client. Return true to accept the client, false to
	 * reject. This method will not be called concurrently between multiple threads.
	 */
	boolean acceptingConnection(NewConnectionContext context);
}
