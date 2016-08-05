package org.gillius.jagnet;

public class TimeSync {
	private int requestId;
	private long sendTime;

	private long rtt;
	private long timeOffset;

	public void send(Connection conn) {
		TimeSyncRequest request = TimeSyncRequest.getNextRequest();
		requestId = request.requestId;
		sendTime = System.nanoTime();
		conn.sendFast(request);
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
