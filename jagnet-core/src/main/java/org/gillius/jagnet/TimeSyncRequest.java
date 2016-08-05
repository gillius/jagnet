package org.gillius.jagnet;

import java.util.concurrent.atomic.AtomicInteger;

public class TimeSyncRequest {
	private static final AtomicInteger nextId = new AtomicInteger();

	public int requestId;

	public static TimeSyncRequest getNextRequest() {
		return new TimeSyncRequest(nextId.getAndIncrement());
	}

	public TimeSyncRequest() {
	}

	public TimeSyncRequest(int requestId) {
		this.requestId = requestId;
	}
}
