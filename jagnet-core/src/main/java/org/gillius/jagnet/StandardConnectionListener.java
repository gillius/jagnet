package org.gillius.jagnet;

public class StandardConnectionListener implements ConnectionListener {
	@Override
	public void onReceive(ConnectionListenerContext ctx, Object message) {
		if (message instanceof TimeSyncRequest) {
			TimeSync.receiveRequestAndRespond(ctx.getConnection(), (TimeSyncRequest) message);
			ctx.consumeCurrentEvent();
		}
	}
}
