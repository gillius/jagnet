package org.gillius.jagnet.netty;

import io.netty.channel.Channel;
import org.gillius.jagnet.Connection;

import java.util.concurrent.CompletableFuture;

public class NettyConnection implements Connection {
	private final Channel channel;
	private final CompletableFuture<Connection> closeFuture = new CompletableFuture<>();

	public NettyConnection(Channel channel) {
		this.channel = channel;
	}

	public Channel getChannel() {
		return channel;
	}

	@Override
	public void sendReliable(Object message) {
		channel.writeAndFlush(message);
	}

	@Override
	public void sendFast(Object message) {
		channel.writeAndFlush(message).addListener(future -> {
			if (!future.isSuccess())
				future.cause().printStackTrace();
		});
	}

	@Override
	public boolean isOpen() {
		return channel.isActive();
	}

	@Override
	public void close() {
		channel.close().addListener(future -> {
			if (future.isSuccess())
				closeFuture.complete(this);
			else
				closeFuture.completeExceptionally(future.cause());
		});
	}

	@Override
	public CompletableFuture<Connection> getCloseFuture() {
		return closeFuture;
	}
}
