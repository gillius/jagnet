package org.gillius.jagnet;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

/**
 * A {@link ConnectionListener} that allows waiting for conditions to complete, which in turn completes a
 * {@link CompletableFuture}. This listener can be registered on a single connection at a time only.
 */
public class ConditionConnectionListener implements ConnectionListener {
	private final ConcurrentLinkedQueue<Condition> conditions = new ConcurrentLinkedQueue<>();

	private AtomicReference<Connection> connection = new AtomicReference<>(null);

	@Override
	public void onConnected(ConnectionListenerContext ctx) {
		if (!connection.compareAndSet(null, ctx.getConnection()))
			throw new IllegalStateException(this + " used to listen to more than one connection");
	}

	@Override
	public void onDisconnected(ConnectionListenerContext ctx) {
		connection.set(null);
		//TODO: handle registrations during and after disconnect
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
		Connection conn = connection.get();
		if (conn == null)
			throw new IllegalStateException("Connection not open");

		Condition cond = new Condition(predicate);
		conditions.add(cond);
		conn.sendReliable(message);
		return cond.future;
	}

	private static class Condition {
		//TODO: some kind of "timeout"
		public final BiPredicate<ConnectionListenerContext, Object> predicate;
		public final CompletableFuture<Object> future = new CompletableFuture<>();

		public Condition(BiPredicate<ConnectionListenerContext, Object> predicate) {
			this.predicate = predicate;
		}
	}
}
