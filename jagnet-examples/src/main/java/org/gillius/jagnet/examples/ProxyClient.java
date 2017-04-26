package org.gillius.jagnet.examples;

import com.esotericsoftware.kryo.Kryo;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.gillius.jagnet.netty.KryoDecoder;
import org.gillius.jagnet.netty.KryoEncoder;
import org.gillius.jagnet.proxy.client.ProxyClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class ProxyClient {
	private static final Logger log = LoggerFactory.getLogger(ProxyClient.class);

	public static void main(String[] args) throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group)
			 .channel(NioSocketChannel.class)
			 .option(ChannelOption.TCP_NODELAY, true)
			 .handler(new ChannelInitializer<SocketChannel>() {
				 @Override
				 public void initChannel(SocketChannel ch) throws Exception {
					 if (args.length > 0 && "websocket".equalsIgnoreCase(args[0])) {
						 log.info("Running in websocket mode");
						 ch.pipeline()
						   .addLast(new HttpClientCodec())
						   .addLast(new HttpObjectAggregator(8192))
						   .addLast(new WebsocketClientHandler());
					 }
					 ch.pipeline()
					   .addLast(new LineBasedFrameDecoder(512, true, true))
					   .addLast(new ProxyClientHandler("thetag", ctx -> {
						   ChannelPipeline p = ctx.pipeline();
						   p.remove(LineBasedFrameDecoder.class);
						   p.remove(ProxyClientHandler.class);
						   p.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
						   p.addLast(new KryoDecoder(makeKryo()));
						   p.addLast(new KryoEncoder(makeKryo()));
						   p.addLast(new LoggingHandler());

						   ch.writeAndFlush(new Message("I'm a message!"));
					   }));
				 }
			 });

			// Start the client.
			ChannelFuture f = b.connect("localhost", 56238).sync();

			// Wait until the connection is closed.
			f.channel().closeFuture().sync();
		} finally {
			// Shut down the event loop to terminate all threads.
			group.shutdownGracefully();
		}
	}

	private static Kryo makeKryo() {
		Kryo kryo = new Kryo();
		kryo.register(Message.class);
		return kryo;
	}

	public static class Message {
		public String message;

		public Message() {
		}

		public Message(String message) {
			this.message = message;
		}

		@Override
		public String toString() {
			return message;
		}
	}

	private static class WebsocketClientHandler extends ChannelDuplexHandler {
		private boolean wsActive = false;

		private final WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
				new URI("ws://127.0.0.1:56238/websocket"),
		    WebSocketVersion.V13, null, false, new DefaultHttpHeaders());

		private WebsocketClientHandler() throws URISyntaxException {
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
				wsActive = true;
				ctx.fireChannelActive();
				return;
			}

			if (msg instanceof FullHttpResponse) {
				FullHttpResponse response = (FullHttpResponse) msg;
				throw new IllegalStateException(
						"Unexpected FullHttpResponse (getStatus=" + response.status() +
						", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
			}

			WebSocketFrame frame = (WebSocketFrame) msg;
			if (frame instanceof BinaryWebSocketFrame) {
				BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) frame;
				ctx.fireChannelRead(binaryFrame.content());

			} else if (frame instanceof CloseWebSocketFrame) {
				System.out.println("WebSocket Client received closing");
				ch.close();

			} else {
				log.warn("Ignoring unrecognized message {}", msg);
			}
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			if (wsActive) {
				ByteBuf buf = (ByteBuf) msg;
				ctx.write(new BinaryWebSocketFrame(buf), promise);
			} else {
				super.write(ctx, msg, promise);
			}
		}
	}
}
