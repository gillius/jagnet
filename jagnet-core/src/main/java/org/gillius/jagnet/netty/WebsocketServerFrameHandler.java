package org.gillius.jagnet.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * This handler replaces itself with {@link WebsocketBinaryFrameCodec} when the
 * {@link io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete} event occurs.
 */
public class WebsocketServerFrameHandler extends ChannelDuplexHandler {
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
			ctx.pipeline().addAfter(ctx.executor(), ctx.name(), null, new WebsocketBinaryFrameCodec());
			ctx.pipeline().remove(this);
		}
		super.userEventTriggered(ctx, evt);
	}
}
