package org.gillius.jagnet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

/**
 * A {@link ConnectionListener} that allows waiting for conditions to complete, which in turn completes a
 * {@link CompletableFuture}. This listener can be registered on a single connection at a time only.
 */
public class ConditionConnectionListener implements ConnectionListener {
	private final List<Condition> conditions = new ArrayList<>();

	private Connection connection;

	@Override
	public void onConnected(ConnectionListenerContext ctx) {
		if (connection != null)
			throw new IllegalStateException(this + " cannot be used to listen to more than one connection");
		connection = ctx.getConnection();
	}

	@Override
	public void onDisconnected(ConnectionListenerContext ctx) {
		for (Condition condition : conditions) {
			condition.future.completeExceptionally(new DisconnectException());
		}
	}

	@Override
	public void onReceive(ConnectionListenerContext ctx, Object message) {
		for (Iterator<Condition> iterator = conditions.iterator(); iterator.hasNext(); ) {
			Condition next = iterator.next();
			if (next.predicate.test(ctx, message)) {
				next.future.complete(message);
				iterator.remove();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T> CompletableFuture<T> sendReliableAndReceive(Object message, Class<T> expectedResponse) {
		return (CompletableFuture<T>) sendReliableAndReceive(message, (ctx, m) -> expectedResponse.isInstance(m));
	}

	public CompletableFuture<Object> sendReliableAndReceive(Object message, BiPredicate<ConnectionListenerContext, Object> predicate) {
		CompletableFuture<Object> future = new CompletableFuture<>();

		if (connection == null) {
			future.completeExceptionally(new DisconnectException());
		} else {
			Condition cond = new Condition(predicate, future);
			connection.execute(() -> {
				conditions.add(cond);
				connection.sendReliable(message);
			});
		}

		return future;
	}

	private static class Condition {
		//TODO: some kind of "timeout"
		public final BiPredicate<ConnectionListenerContext, Object> predicate;
		public final CompletableFuture<Object> future;

		public Condition(BiPredicate<ConnectionListenerContext, Object> predicate, CompletableFuture<Object> future) {
			this.predicate = predicate;
			this.future = future;
		}
	}
}
