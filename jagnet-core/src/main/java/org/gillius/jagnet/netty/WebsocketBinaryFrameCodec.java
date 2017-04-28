package org.gillius.jagnet.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

/**
 * Wraps and unwraps websocket binary frames to and from ByteBuf.
 */
public class WebsocketBinaryFrameCodec extends ChannelDuplexHandler {
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
		ctx.fireChannelRead(frame.content());
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		ctx.write(new BinaryWebSocketFrame((ByteBuf) msg), promise);
	}
}
