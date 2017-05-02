package org.gillius.jagnet.proxy.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundInvoker;
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
			log.debug("Received good single-connect proxy header from client");
			state = this::tagState;

		} else if (ProxyConstants.INTRO_HEADER_SERVICE.equals(line)) {
			log.debug("Received good service proxy header from client");
			state = this::registerState;

		} else {
			log.error("Bad header {}", line);
			ctx.close();
			state = this::discardState;
		}
	}

	private void tagState(ChannelHandlerContext ctx, String line) {
		Promise<Channel> promise = ctx.executor().newPromise();
		log.info("New connection for tag {} from {}", line, ctx.channel());
		if (line.startsWith(ProxyConstants.SERVICE_PREFIX)) {
			if (!proxyMap.matchService(line, ctx.channel(), promise)) {
				writeString(ctx, ProxyConstants.NO_SUCH_SERVICE);
				ctx.close();
				state = this::discardState;
				return;
			}
		} else {
			proxyMap.matchChannel(line, ctx.channel(), promise);
		}

		writeString(ctx, ProxyConstants.WAITING_FOR_REMOTE);
		state = this::discardState;
		promise.addListener(future -> {
			if (future.isSuccess()) {
				log.info("Starting up new proxy tunnel from {} to {}", ctx.channel(), future.getNow());
				writeString(ctx, ProxyConstants.CONNECTED);
				ctx.pipeline().remove(LineBasedFrameDecoder.class);
				ctx.pipeline().remove(this);
				ctx.pipeline().addLast(new ProxyHandler((Channel) future.getNow()));
			}
		});
	}

	private void registerState(ChannelHandlerContext ctx, String line) {
		if (!line.startsWith(ProxyConstants.SERVICE_PREFIX)) {
			log.info("Invalid service tag {} from {}", line, ctx.channel());
			writeString(ctx, ProxyConstants.INVALID_TAG);
			ctx.close();

		} else if (proxyMap.registerService(line, ctx.channel(), ProxyServerHandler::writeTag)) {
			log.info("New service listening for tag {} from {}", line, ctx.channel());
			ctx.channel().closeFuture().addListener(f -> log.info("service {} closed from {}", line, ctx.channel()));
			writeString(ctx, ProxyConstants.WAITING_FOR_REMOTE);

		} else {
			log.info("Attempt to register service listener for {} from {}, but already taken", line, ctx.channel());
			writeString(ctx, ProxyConstants.SERVICE_TAKEN);
			ctx.close();
		}

		state = this::discardState;
	}

	@SuppressWarnings("unused")
	private void discardState(ChannelHandlerContext ctx, String line) {}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		log.error("Error in pipeline; closing connection", cause);
		ctx.close();
	}

	private static void writeTag(ChannelOutboundInvoker ctx, String tag) {
		log.info("Assigning new connection {} to {}", tag, ctx);
		writeString(ctx, tag);
	}

	private static void writeString(ChannelOutboundInvoker ctx, String string) {
		ctx.writeAndFlush(Unpooled.copiedBuffer(string + '\n', CharsetUtil.UTF_8));
	}
}
