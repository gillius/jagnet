package org.gillius.jagnet.proxy.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Promise;
import org.gillius.jagnet.proxy.ProxyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;

class ProxyServerHandler extends ByteToMessageDecoder {
	private static final Logger log = LoggerFactory.getLogger(ProxyServerHandler.class);

	private BiConsumer<ChannelHandlerContext, String> state = this::headerState;
	private final ProxyMap proxyMap;

	ProxyServerHandler(ProxyMap proxyMap) {
		this.proxyMap = proxyMap;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		int len = in.readableBytes();
		String line = in.readCharSequence(len, CharsetUtil.UTF_8).toString();
		state.accept(ctx, line);
	}

	private void headerState(ChannelHandlerContext ctx, String line) {
		if (ProxyConstants.INTRO_HEADER.equals(line)) {
			log.debug("Received good header from client");
			state = this::tagState;
		} else {
			log.error("Bad header {}", line);
			ctx.close();
		}
	}

	private void tagState(ChannelHandlerContext ctx, String line) {
		Promise<Channel> promise = ctx.executor().newPromise();
		log.info("New connection for tag {} from {}", line, ctx.channel());
		proxyMap.matchChannel(line, ctx.channel(), promise);
		writeString(ctx, ProxyConstants.WAITING_FOR_REMOTE + "\n");
		state = this::proxyState;
		promise.addListener(future -> {
			if (future.isSuccess()) {
				log.info("Starting up new proxy tunnel from {} to {}", ctx.channel(), future.getNow());
				writeString(ctx, ProxyConstants.CONNECTED + "\n");
				ctx.pipeline().remove(LineBasedFrameDecoder.class);
				ctx.pipeline().remove(this);
				ctx.pipeline().addLast(new ProxyHandler((Channel) future.getNow()));
//						ctx.pipeline().addLast(new LoggingHandler());
			}
		});
	}

	private void proxyState(ChannelHandlerContext ctx, String line) {
		log.warn("Discarded line since proxy listener still active after tunnel established");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		log.error("Error in pipeline; closing connection", cause);
		ctx.close();
	}

	private static void writeString(ChannelHandlerContext ctx, String string) {
		ctx.writeAndFlush(Unpooled.copiedBuffer(string, CharsetUtil.UTF_8));
	}
}
