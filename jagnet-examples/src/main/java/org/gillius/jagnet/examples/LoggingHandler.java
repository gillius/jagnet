package org.gillius.jagnet.examples;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class LoggingHandler extends ChannelDuplexHandler {
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Session Started");
		super.channelActive(ctx);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Handler added");
		super.handlerAdded(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//		ByteBuf m = (ByteBuf) msg;
//		System.out.println(m.readCharSequence(m.readableBytes(), CharsetUtil.US_ASCII));
		System.out.println("Read: " + msg);
		super.channelRead(ctx, msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		System.out.println("Write: " + msg);
		super.write(ctx, msg, promise);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Session Ended");
		super.channelInactive(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.err.println(cause);
		super.exceptionCaught(ctx, cause);
	}
}
