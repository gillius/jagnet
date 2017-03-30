package org.gillius.jagnet;

public class TimeSync {
	private int requestId;
	private long sendTime;

	private long rtt;
	private long timeOffset;

	/**
	 * Returns a message to be sent, which should be sent immediately as the send time is tracked.
	 */
	public TimeSyncRequest send() {
		TimeSyncRequest request = TimeSyncRequest.getNextRequest();
		requestId = request.requestId;
		sendTime = System.nanoTime();
		return request;
	}

	public void send(Connection conn) {
		conn.sendFast(send());
	}

	public static void receiveRequestAndRespond(Connection connection, TimeSyncRequest request) {
		long receiveTime = System.nanoTime();
		TimeSyncResponse response = new TimeSyncResponse();
		response.receiveTime = receiveTime;
		response.requestId = request.requestId;
		response.sendTime = System.nanoTime();
		connection.sendFast(response);
	}

	public void receive(TimeSyncResponse response) {
		long receiveTime = System.nanoTime();

		if (response.requestId == requestId) {
			rtt = (receiveTime - sendTime) - (response.sendTime - response.receiveTime);
			timeOffset = ( (response.receiveTime - sendTime) + (response.sendTime - receiveTime) ) / 2;
		}
	}

	public long getRtt() {
		return rtt;
	}

	public long getTimeOffset() {
		return timeOffset;
	}

	public long getMaxTimeOffsetError() {
		return rtt / 2;
	}
}
