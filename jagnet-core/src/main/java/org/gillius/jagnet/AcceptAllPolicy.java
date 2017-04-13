package org.gillius.jagnet;

public class AcceptAllPolicy implements AcceptPolicy {
	public static final AcceptAllPolicy INSTANCE = new AcceptAllPolicy();

	@Override
	public boolean acceptingConnection(NewConnectionContext context) {
		return true;
	}
}
