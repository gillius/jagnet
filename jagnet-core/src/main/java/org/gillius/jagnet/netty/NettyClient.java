package org.gillius.jagnet.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import org.gillius.jagnet.Client;
import org.gillius.jagnet.Connection;
import org.gillius.jagnet.ConnectionListener;
import org.gillius.jagnet.NoopConnectionListener;
import org.gillius.jagnet.proxy.client.ProxyClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class NettyClient implements Client {
	private static final Logger log = LoggerFactory.getLogger(NettyClient.class);

	private int port = -1;
	private String host = null;
	private String proxyTag = null;
	private final KryoBuilder kryoBuilder = new KryoBuilder();
	private ConnectionListener listener = NoopConnectionListener.INSTANCE;

	private NettyConnection connection;
	private CompletableFuture<Connection> connFuture = new CompletableFuture<>();
	private EventLoopGroup group;

	@Override
	public void setPort(int port) {
		this.port = port;
	}

	public void setProxyTag(String proxyTag) {
		this.proxyTag = proxyTag;
	}

	@Override
	public void registerMessages(Iterable<Class<?>> messageTypes) {
		kryoBuilder.registerMessages(messageTypes);
	}

	@Override
	public void registerMessages(Class<?>... messageTypes) {
		kryoBuilder.registerMessages(messageTypes);
	}

	@Override
	public void close() {
		if (connection != null)
			connection.close();
		if (group != null)
			group.shutdownGracefully();
		connection = null;
		group = null;
	}

	@Override
	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public void setListener(ConnectionListener listener) {
		this.listener = listener;
	}

	@Override
	public void start() {
		group = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(group)
		 .channel(NioSocketChannel.class)
		 .option(ChannelOption.TCP_NODELAY, true)
		 .handler(new ChannelInitializer<SocketChannel>() {
			 @Override
			 public void initChannel(SocketChannel ch) throws Exception {
				 connection = new NettyConnection(ch);
				 connection.getCloseFuture().thenRun(NettyClient.this::close);
				 if (proxyTag != null) {
					 ch.pipeline()
					   .addLast(new LineBasedFrameDecoder(512, true, true))
					   .addLast(new ProxyClientHandler(proxyTag, ctx -> {
						   log.info("Switching from proxy mode");
						   ChannelPipeline p = ctx.pipeline();
						   p.remove(LineBasedFrameDecoder.class);
						   p.remove(ProxyClientHandler.class);
						   setupPipeline(ch);
					   }));
				 } else {
					 setupPipeline(ch);
				 }
			 }
		 });

		// Start the client.
		b.connect(host, port);
	}

	@Override
	public CompletableFuture<Connection> getConnection() {
		return connFuture;
	}

	protected void setupPipeline(SocketChannel ch) {
		ch.pipeline()
		  .addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2))
		  .addLast(new KryoDecoder(kryoBuilder.get()))
		  .addLast(new KryoEncoder(kryoBuilder.get()))
		  //TODO: setListener after opening connection?
		  .addLast(new NettyHandler(connection, listener, connFuture))
//		  .addLast(new LoggingHandler())
		;
	}
}