package org.gillius.jagnet.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Triggers a websocket handshake when channel becomes active, and suppresses channel active event for later listeners
 * until handshake completes. When handshake completes, processing is handled by {@link WebsocketBinaryFrameCodec}.
 */
public class WebsocketClientHandler extends ChannelDuplexHandler {
	private final WebSocketClientHandshaker handshaker;

	public WebsocketClientHandler(URI destination) throws URISyntaxException {
		handshaker = WebSocketClientHandshakerFactory.newHandshaker(
				destination,
				WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		handshaker.handshake(ctx.channel());
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Channel ch = ctx.channel();
		if (!handshaker.isHandshakeComplete()) {
			handshaker.finishHandshake(ch, (FullHttpResponse) msg);
			ctx.pipeline().addAfter(ctx.executor(), ctx.name(), null, new WebsocketBinaryFrameCodec());
			ctx.pipeline().remove(this);
			ctx.fireChannelActive();
		} else {
			throw new IllegalStateException("Handshake is completed but this handler still exists");
		}
	}
}
