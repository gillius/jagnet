package org.gillius.jagnet.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.gillius.jagnet.ConnectionListener;
import org.gillius.jagnet.ConnectionStateListener;
import org.gillius.jagnet.SingleConnectionListenerContext;

class NettyHandler extends ChannelInboundHandlerAdapter {
	private final SingleConnectionListenerContext clc;
	private final ConnectionListener listener;
	private final ConnectionStateListener connectionStateListener;

	public NettyHandler(NettyConnection connection, ConnectionListener listener) {
		this(connection, listener, null);
	}

	public NettyHandler(NettyConnection connection, ConnectionListener listener, ConnectionStateListener connectionStateListener) {
		clc = connection.getContext();
		this.listener = listener;
		this.connectionStateListener = connectionStateListener;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		listener.onConnected(clc);
		super.channelActive(ctx);
		if (connectionStateListener != null)
			connectionStateListener.onConnected(clc);
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
		if (connectionStateListener != null)
			connectionStateListener.onDisconnected(clc);
	}
}
