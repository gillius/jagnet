package org.gillius.jagnet.netty;

import io.netty.channel.Channel;
import org.gillius.jagnet.Connection;
import org.gillius.jagnet.SingleConnectionListenerContext;

import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class NettyConnection implements Connection {
	private final SingleConnectionListenerContext context = new SingleConnectionListenerContext(this);
	private final Channel channel;
	private final CompletableFuture<Connection> closeFuture = new CompletableFuture<>();

	public NettyConnection(Channel channel) {
		this.channel = channel;
		channel.closeFuture().addListener(future -> {
			if (future.isSuccess())
				closeFuture.complete(this);
			else
				closeFuture.completeExceptionally(future.cause());
		});
	}

	public SingleConnectionListenerContext getContext() {
		return context;
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
	public SocketAddress getLocalAddress() {
		return channel.localAddress();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return channel.remoteAddress();
	}

	@Override
	public boolean isOpen() {
		return channel.isActive();
	}

	@Override
	public void close() {
		channel.close();
	}

	@Override
	public CompletableFuture<Connection> getCloseFuture() {
		return closeFuture;
	}

	@Override
	public void execute(Runnable task) {
		channel.eventLoop().execute(task);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return channel.eventLoop().submit(task);
	}
}
