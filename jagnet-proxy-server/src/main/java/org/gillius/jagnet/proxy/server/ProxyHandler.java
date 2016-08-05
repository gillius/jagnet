package org.gillius.jagnet.proxy.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ProxyHandler extends ChannelInboundHandlerAdapter {
	private final Channel dest;

	public ProxyHandler(Channel dest) {
		this.dest = dest;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		dest.writeAndFlush(msg);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		dest.close();
	}
}
