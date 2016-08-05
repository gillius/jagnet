package org.gillius.jagnet.proxy.server;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;

import java.util.HashMap;
import java.util.Map;

public class ProxyMap {
	private final Map<String, Waiter> waiters = new HashMap<>();

	public synchronized boolean matchChannel(String id, Channel channel, Promise<Channel> otherChannel) {
		Waiter thisWaiter = new Waiter(channel, otherChannel);
		Waiter existing = waiters.get(id);
		if (existing != null) {
			//We found a match
			waiters.remove(id);
			thisWaiter.promise.setSuccess(existing.channel);
			existing.promise.setSuccess(thisWaiter.channel);
			return true;
		} else {
			waiters.put(id, thisWaiter);
			return false;
		}
	}

	private static class Waiter {
		final Channel channel;
		final Promise<Channel> promise;

		Waiter(Channel channel, Promise<Channel> promise) {
			this.channel = channel;
			this.promise = promise;
		}
	}
}
