package org.gillius.jagnet.proxy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServer implements AutoCloseable {
	private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);

	private int port = 56238;

	private Runnable closer;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void start() {
		ProxyMap proxyMap = new ProxyMap();
		ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

		EventLoopGroup group = new NioEventLoopGroup(1);
		ServerBootstrap b = new ServerBootstrap();
		b.group(group)
		 .channel(NioServerSocketChannel.class)
		 .childHandler(new ChannelInitializer<SocketChannel>() {
			 @Override
			 public void initChannel(SocketChannel ch) throws Exception {
				 ch.pipeline()
				   .addLast(new LineBasedFrameDecoder(512, true, true))
				   .addLast(new ProxyServerHandler(proxyMap))
				 ;
			 }
		 })
		 .option(ChannelOption.SO_BACKLOG, 128)
		 .childOption(ChannelOption.TCP_NODELAY, true)
		 .childOption(ChannelOption.SO_KEEPALIVE, true);

		// Bind and start to accept incoming connections.
		ChannelFuture f = b.bind(port).syncUninterruptibly();
		log.info("Proxy server started on port {}", port);
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

		proxyServer.start();
	}
}
