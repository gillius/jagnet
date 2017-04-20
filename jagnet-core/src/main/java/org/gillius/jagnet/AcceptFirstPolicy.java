package org.gillius.jagnet;

/**
 * Accepts only the first connection to the server.
 */
public class AcceptFirstPolicy implements AcceptPolicy {
	private boolean accepting = true;

	@Override
	public boolean acceptingConnection(NewConnectionContext context) {
		boolean ret = accepting;
		accepting = false;
		return ret;
	}
}
