package org.gillius.jagnet;

import java.net.InetSocketAddress;

public class AcceptAllPolicy implements AcceptPolicy {
	public static final AcceptAllPolicy INSTANCE = new AcceptAllPolicy();

	@Override
	public boolean acceptingConnection(InetSocketAddress address) {
		return true;
	}
}
