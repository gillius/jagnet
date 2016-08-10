package org.gillius.jagnet;

import java.net.InetSocketAddress;

public interface AcceptPolicy {
	/**
	 * Event when the server is about to accept a new connection from a client. Return true to accept the client, false to
	 * reject.
	 */
	boolean acceptingConnection(InetSocketAddress address);
}
