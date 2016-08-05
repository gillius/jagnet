package org.gillius.jagnet.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.gillius.jagnet.Connection;
import org.gillius.jagnet.ConnectionListener;

import java.util.concurrent.CompletableFuture;

class NettyHandler extends ChannelInboundHandlerAdapter {
	private final NettyConnection connection;
	private final ConnectionListener listener;
	private final CompletableFuture<Connection> connFuture;

	public NettyHandler(NettyConnection connection, ConnectionListener listener, CompletableFuture<Connection> connFuture) {
		this.connection = connection;
		this.listener = listener;
		this.connFuture = connFuture;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		listener.onConnected(connection);
		connFuture.complete(connection);
		super.channelActive(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		listener.onReceive(connection, msg);
		super.channelRead(ctx, msg);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		listener.onDisconnected(connection);
		super.channelInactive(ctx);
	}
}
