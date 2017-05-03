package org.gillius.jagnet.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ProxyRemoteServerHandler extends ByteToMessageDecoder {
	private static final Logger log = LoggerFactory.getLogger(ProxyRemoteServerHandler.class);

	private final String tag;
	private final Consumer<String> onIncomingConnection;
	private final CompletableFuture<?> registeredFuture;

	private BiConsumer<ChannelHandlerContext, String> state = this::waitForAck;

	public ProxyRemoteServerHandler(String tag, Consumer<String> onIncomingConnection, CompletableFuture<?> registeredFuture) {
		this.tag = tag;
		this.onIncomingConnection = onIncomingConnection;
		this.registeredFuture = registeredFuture;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		ctx.writeAndFlush(Unpooled.copiedBuffer(ProxyConstants.INTRO_HEADER_SERVICE + "\n" +
		                                        ProxyConstants.SERVICE_PREFIX + tag + "\n",
		                                        CharsetUtil.UTF_8));
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		int len = in.readableBytes();
		String line = in.readCharSequence(len, CharsetUtil.UTF_8).toString();
		state.accept(ctx, line);
	}

	private void waitForAck(ChannelHandlerContext ctx, String line) {
		switch (line) {
			case ProxyConstants.WAITING_FOR_REMOTE:
				log.info("Service {} registered, waiting", tag);
				registeredFuture.complete(null);
				state = this::waitConnections;
				break;

			case ProxyConstants.SERVICE_TAKEN:
				String msg = "Service " + tag + " already registered on remote";
				log.warn(msg);
				registeredFuture.completeExceptionally(new IllegalStateException(msg));
				ctx.close();
				break;

			default:
				log.error("Bad response {}", line);
				ctx.close();
				break;
		}
	}

	@SuppressWarnings("unused")
	private void waitConnections(ChannelHandlerContext ctx, String line) {
		onIncomingConnection.accept(line);
	}
}
