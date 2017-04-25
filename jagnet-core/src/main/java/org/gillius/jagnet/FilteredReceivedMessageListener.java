package org.gillius.jagnet;

import java.util.Objects;

public class FilteredReceivedMessageListener<T> implements ReceivedMessageListener<T> {
	private final Class<? extends T> filter;
	private final ReceivedMessageListener<T> listener;

	public static <T> FilteredReceivedMessageListener<T> typedListener(Class<? extends T> filter, ReceivedMessageListener<T> listener) {
		return new FilteredReceivedMessageListener<>(filter, listener);
	}

	public FilteredReceivedMessageListener(Class<? extends T> filter, ReceivedMessageListener<T> listener) {
		Objects.requireNonNull(filter);
		Objects.requireNonNull(listener);
		this.filter = filter;
		this.listener = listener;
	}

	@Override
	public void onReceive(ConnectionListenerContext ctx, T message) {
		if (filter.isInstance(message))
			listener.onReceive(ctx, message);
	}
}
