package org.gillius.jagnet.proxy.server;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.gillius.jagnet.proxy.ProxyConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

public class ProxyMap {
	private final Map<String, Service> services = new HashMap<>();
	private final Map<String, Waiter> waiters = new HashMap<>();
	private final Random random = new Random();

	public synchronized boolean matchService(String id, Channel channel, Promise<Channel> otherChannel) {
		Service service = services.get(id);
		if (service != null) {
			//Select a new, unused tag slot
			String newtag = "";
			while (newtag.isEmpty() || waiters.containsKey(newtag))
				newtag = ProxyConstants.DYNAMIC_PREFIX + random.nextLong();

			matchChannel(newtag, channel, otherChannel);
			service.tagHandler.accept(service.channel, newtag);
			return true;

		} else {
			return false;
		}
	}

	public synchronized void matchChannel(String id, Channel channel, Promise<Channel> otherChannel) {
		Waiter thisWaiter = new Waiter(channel, otherChannel);
		Waiter existing = waiters.get(id);
		//TODO: handle case where the waiter leaves
		if (existing != null) {
			//We found a match
			waiters.remove(id);
			thisWaiter.promise.setSuccess(existing.channel);
			existing.promise.setSuccess(thisWaiter.channel);
		} else {
			waiters.put(id, thisWaiter);
		}
	}

	public synchronized boolean registerService(String id, Channel channel, BiConsumer<Channel, String> tagHandler) {
		if (services.containsKey(id)) {
			return false;

		} else {
			services.put(id, new Service(channel, tagHandler));
			channel.closeFuture().addListener(future -> services.remove(id));
			return true;
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

	private static class Service {
		final Channel channel;
		final BiConsumer<Channel, String> tagHandler;

		public Service(Channel channel, BiConsumer<Channel, String> tagHandler) {
			this.channel = channel;
			this.tagHandler = tagHandler;
		}
	}
}
