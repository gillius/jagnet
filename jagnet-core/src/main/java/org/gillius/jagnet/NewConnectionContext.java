package org.gillius.jagnet;

import java.net.InetSocketAddress;

/**
 * Defines the context of a newly formed connection.
 */
public interface NewConnectionContext {
	InetSocketAddress getLocalAddress();
	InetSocketAddress getRemoteAddress();
}
