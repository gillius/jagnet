package org.gillius.jagnet.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.gillius.jagnet.Connection;
import org.gillius.jagnet.ConnectionListener;
import org.gillius.jagnet.SingleConnectionListenerContext;

import java.util.concurrent.CompletableFuture;

class NettyHandler extends ChannelInboundHandlerAdapter {
	private final SingleConnectionListenerContext clc;
	private final ConnectionListener listener;
	private final CompletableFuture<Connection> connFuture;

	public NettyHandler(NettyConnection connection, ConnectionListener listener, CompletableFuture<Connection> connFuture) {
		clc = new SingleConnectionListenerContext(connection);
		this.listener = listener;
		this.connFuture = connFuture;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		listener.onConnected(clc);
		connFuture.complete(clc.getConnection());
		super.channelActive(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		listener.onReceive(clc, msg);
		super.channelRead(ctx, msg);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		listener.onDisconnected(clc);
		super.channelInactive(ctx);
	}
}
