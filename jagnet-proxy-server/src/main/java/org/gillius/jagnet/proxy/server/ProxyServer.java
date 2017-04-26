package org.gillius.jagnet.proxy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServer implements AutoCloseable {
	private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);

	private int port = 56238;
	private boolean websocketMode = false;

	private Runnable closer;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isWebsocketMode() {
		return websocketMode;
	}

	public void setWebsocketMode(boolean websocketMode) {
		this.websocketMode = websocketMode;
	}

	public void start() {
		ProxyMap proxyMap = new ProxyMap();
		ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

		EventLoopGroup group = new NioEventLoopGroup(1);
		ServerBootstrap b = new ServerBootstrap();
		b.group(group)
		 .channel(NioServerSocketChannel.class)
		 .childHandler(websocketMode ? new WebsocketProxyChannelInitializer(proxyMap) :
		               new TcpProxyChannelInitializer(proxyMap))
		 .option(ChannelOption.SO_BACKLOG, 128)
		 .childOption(ChannelOption.TCP_NODELAY, true)
		 .childOption(ChannelOption.SO_KEEPALIVE, true);

		// Bind and start to accept incoming connections.
		ChannelFuture f = b.bind(port).syncUninterruptibly();
		String mode = websocketMode ? "websocket" : "TCP";
		log.info("Proxy server started on port {} in {} mode", port, mode);
		allChannels.add(f.channel());

		closer = () -> {
			log.info("Shutting down server on request");
			allChannels.close();
			group.shutdownGracefully().syncUninterruptibly();
		};

		Runtime.getRuntime().addShutdownHook(new Thread(this::close));
	}

	@Override
	public void close() {
		if (closer != null)
			closer.run();
		closer = null;
	}

	public static void main(String[] args) throws Exception {
		ProxyServer proxyServer = new ProxyServer();

		if (args.length > 0)
			proxyServer.setPort(Integer.parseInt(args[0]));

		boolean websocketMode = args.length > 1 && "websocket".equalsIgnoreCase(args[1]);
		proxyServer.setWebsocketMode(websocketMode);

		proxyServer.start();
	}

	private static class TcpProxyChannelInitializer extends ChannelInitializer<SocketChannel> {
		private final ProxyMap proxyMap;

		public TcpProxyChannelInitializer(ProxyMap proxyMap) {
			this.proxyMap = proxyMap;
		}

		@Override
		public void initChannel(SocketChannel ch) throws Exception {
			ch.pipeline()
			  .addLast(new LineBasedFrameDecoder(512, true, true))
			  .addLast(new ProxyServerHandler(proxyMap))
			;
		}
	}

	private static class WebsocketProxyChannelInitializer extends ChannelInitializer<SocketChannel> {
		private final ProxyMap proxyMap;

		public WebsocketProxyChannelInitializer(ProxyMap proxyMap) {
			this.proxyMap = proxyMap;
		}

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ch.pipeline()
		    .addLast(new HttpServerCodec())
		    .addLast(new HttpObjectAggregator(65536))
		    .addLast(new WebSocketServerProtocolHandler("/websocket", null, true))
		    .addLast(new WebsocketFrameHandler())
		    .addLast(new LineBasedFrameDecoder(512, true, true))
			  .addLast(new ProxyServerHandler(proxyMap))
			;
		}
	}

	private static class WebsocketFrameHandler extends ChannelDuplexHandler {
		private boolean wsActive = false;

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
				WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
				wsActive = true;
			}
			super.userEventTriggered(ctx, evt);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof BinaryWebSocketFrame) {
				BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
				ctx.fireChannelRead(frame.content());
			} else {
				super.channelRead(ctx, msg);
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
