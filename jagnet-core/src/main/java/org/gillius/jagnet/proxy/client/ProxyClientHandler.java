package org.gillius.jagnet.proxy.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.gillius.jagnet.proxy.ProxyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class ProxyClientHandler extends ByteToMessageDecoder {
	private static final Logger log = LoggerFactory.getLogger(ProxyClientHandler.class);

	private final String tag;
	private final Consumer<ChannelHandlerContext> onConnected;

	public ProxyClientHandler(String tag, Consumer<ChannelHandlerContext> onConnected) {
		this.tag = tag;
		this.onConnected = onConnected;
	}

	private State state = State.WAIT_ACK;

	private enum State {
		WAIT_ACK, WAIT_START
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		ctx.writeAndFlush(
				Unpooled.copiedBuffer(ProxyConstants.INTRO_HEADER + "\n" + tag + "\n", CharsetUtil.UTF_8));
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		int len = in.readableBytes();
		String line = in.readCharSequence(len, CharsetUtil.UTF_8).toString();
		switch(state) {
			case WAIT_ACK:
				if (ProxyConstants.WAITING_FOR_REMOTE.equals(line)) {
					log.info("Good tag response, waiting");
					state = State.WAIT_START;
				} else {
					log.error("Bad response {}", line);
					ctx.close();
				}
				break;

			case WAIT_START:
				if (ProxyConstants.CONNECTED.equals(line)) {
					log.info("Match found; connected");
					onConnected.accept(ctx);
				} else {
					log.error("Bad response {}", line);
					ctx.close();
				}
				break;
		}
	}
}
