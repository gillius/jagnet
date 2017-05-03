package org.gillius.jagnet.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import org.gillius.jagnet.*;
import org.gillius.jagnet.proxy.ProxyRemoteServerHandler;
import org.gillius.jagnet.proxy.client.ProxyClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class NettyRemoteServer implements Server {
	private static final Logger log = LoggerFactory.getLogger(NettyRemoteServer.class);

	private String proxyHost = null;
	private int port = -1;
	private String serviceTag = null;
	private final KryoBuilder kryoBuilder = new KryoBuilder();
	private Function<NewConnectionContext, ConnectionListener> listenerFactory = null;
	private ConnectionStateListener connectionStateListener = NoopConnectionListener.INSTANCE;
	private final CompletableFuture<Object> registeredFuture = new CompletableFuture<>();

	private EventLoopGroup group;

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setServiceTag(String serviceTag) {
		this.serviceTag = serviceTag;
	}

	@Override
	public AcceptPolicy getAcceptPolicy() {
		return AcceptAllPolicy.INSTANCE;
	}

	@Override
	public void setAcceptPolicy(AcceptPolicy acceptPolicy) {
		throw new UnsupportedOperationException("Not supported for remote server connections");
	}

	public void registerMessages(Iterable<Class<?>> messageTypes) {
		kryoBuilder.registerMessages(messageTypes);
	}

	public void registerMessages(Class<?>... messageTypes) {
		kryoBuilder.registerMessages(messageTypes);
	}

	public void setListenerFactory(Function<NewConnectionContext, ConnectionListener> factory) {
		this.listenerFactory = factory;
	}

	public ConnectionStateListener getConnectionStateListener() {
		return connectionStateListener;
	}

	public void setConnectionStateListener(ConnectionStateListener connectionStateListener) {
		this.connectionStateListener = connectionStateListener;
	}

	/**
	 * Returns a future that is completed successfully with an undefined value (possibly null) when the service is
	 * registered and listening, or completed exceptionally when it does not.
	 */
	public CompletableFuture<Object> getRegisteredFuture() {
		return registeredFuture;
	}

	public void start() {
		if (listenerFactory == null)
			throw new IllegalStateException("listenerFactory not initialized");
		if (port < 0)
			throw new IllegalStateException("port not initialized");
		if (proxyHost == null)
			throw new IllegalStateException("proxyHost not initialized");
		if (serviceTag == null)
			throw new IllegalStateException("serviceTag not initialized");

		group = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(group)
		 .channel(NioSocketChannel.class)
		 .option(ChannelOption.TCP_NODELAY, true)
		 .handler(new ChannelInitializer<SocketChannel>() {
			 @Override
			 public void initChannel(SocketChannel ch) throws Exception {
//				 if (protocol == Protocol.WS) {
//					 ch.pipeline()
//					   .addLast(new HttpClientCodec())
//					   .addLast(new HttpObjectAggregator(8192))
//					   //TODO: take URI parameter
//					   .addLast(new WebsocketClientHandler(new URI("ws", null, host, port, "/websocket", null, null)));
//				 }

				 ch.pipeline()
				   .addLast(new LineBasedFrameDecoder(512, true, true))
				   .addLast(new ProxyRemoteServerHandler(serviceTag, NettyRemoteServer.this::addClient, registeredFuture));
			 }
		 });

		try {
			b.connect(proxyHost, port).sync();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void stopAcceptingNewConnections() {
		throw new UnsupportedOperationException("Not yet supported"); //TODO: support
	}

	@Override
	public void close() {
		throw new UnsupportedOperationException("Not yet supported"); //TODO: support
	}

	private void addClient(String tag) {
		log.info("Received incoming connection with tag {}", tag);
		Bootstrap b = new Bootstrap();
		b.group(group)
		 .channel(NioSocketChannel.class)
		 .option(ChannelOption.TCP_NODELAY, true)
		 .handler(new ChannelInitializer<SocketChannel>() {
			 @Override
			 public void initChannel(SocketChannel ch) throws Exception {
//				 if (protocol == Protocol.WS) {
//					 ch.pipeline()
//					   .addLast(new HttpClientCodec())
//					   .addLast(new HttpObjectAggregator(8192))
//					   //TODO: take URI parameter
//					   .addLast(new WebsocketClientHandler(new URI("ws", null, host, port, "/websocket", null, null)));
//				 }

				 if (tag != null) { //TODO: share with NettyClient?
					 ch.pipeline()
					   .addLast(new LineBasedFrameDecoder(512, true, true))
					   .addLast(new ProxyClientHandler(tag, ctx -> {
						   log.info("Switching from proxy mode");
						   ChannelPipeline p = ctx.pipeline();
						   p.remove(LineBasedFrameDecoder.class);
						   p.remove(ProxyClientHandler.class);
						   setupPipeline(ch);
						   ch.pipeline().fireChannelActive();
					   }));
				 } else {
					 setupPipeline(ch);
				 }
			 }
		 });

		// Start the client.
		b.connect(proxyHost, port); //TODO: handle errors?
	}

	protected void setupPipeline(SocketChannel ch) {
		ch.pipeline()
		  .addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2))
		  .addLast(new KryoDecoder(kryoBuilder.get()))
		  .addLast(new KryoEncoder(kryoBuilder.get(), true))
		  .addLast(new NettyHandler(new NettyConnection(ch), listenerFactory.apply(new NettyNewConnectionContext(ch)), connectionStateListener));
	}
}
