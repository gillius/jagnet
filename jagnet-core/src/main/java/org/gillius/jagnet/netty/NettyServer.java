package org.gillius.jagnet.netty;

import com.esotericsoftware.kryo.Kryo;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.gillius.jagnet.*;
import org.gillius.jagnet.proxy.ProxyRemoteServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class NettyServer implements Server {
	private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

	private ConnectionParams params;
	private AcceptPolicy acceptPolicy = AcceptAllPolicy.INSTANCE;
	private ConnectionStateListener connectionStateListener = NoopConnectionListener.INSTANCE;
	private final CompletableFuture<Object> listeningFuture = new CompletableFuture<>();

	private ChannelGroup allChannels;
	private EventLoopGroup group;
	private Channel serverChannel;

	@Override
	public AcceptPolicy getAcceptPolicy() {
		return acceptPolicy;
	}

	@Override
	public void setAcceptPolicy(AcceptPolicy acceptPolicy) {
		if (acceptPolicy == null)
			acceptPolicy = AcceptAllPolicy.INSTANCE;
		this.acceptPolicy = acceptPolicy;
	}

	public ConnectionStateListener getConnectionStateListener() {
		return connectionStateListener;
	}

	public void setConnectionStateListener(ConnectionStateListener connectionStateListener) {
		this.connectionStateListener = connectionStateListener;
	}

	/**
	 * Returns a future that is completed successfully with an undefined value (possibly null) when the service is
	 * bound and listening (local server) or registered and listening (remote proxy), or completed exceptionally if there
	 * listening fails.
	 */
	public CompletableFuture<Object> getListeningFuture() {
		return listeningFuture;
	}

	public void start(ConnectionParams params) {
		if (params.getListenerFactory() == null)
			throw new IllegalArgumentException("listenerFactory not initialized");

		boolean remoteMode = params.isProxyMode() || params.getRemoteAddress() != null;
		if (remoteMode) {
			if (acceptPolicy != null && !(acceptPolicy instanceof AcceptAllPolicy))
				throw new IllegalArgumentException("remote mode: only 'accept all' policy allowed");
			if (params.getRemoteAddress() == null)
				throw new IllegalArgumentException("remote mode: remoteAddress not initialized");
			if (params.getProxyTag() == null)
				throw new IllegalArgumentException("remote mode: proxyTag not initialized");
		} else {
			if (params.getLocalAddress() == null)
				throw new IllegalArgumentException("local mode: localAddress not initialized");
		}

		this.params = params.clone();

		group = new NioEventLoopGroup(1);
		allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

		ChannelFuture future = remoteMode ? startRemoteListening() : startLocalListening();

		serverChannel = future.channel();
		allChannels.add(serverChannel);
		try {
			future.sync(); //sync to wait here for bind to complete or connect to start, so we get exception if it fails
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public ChannelFuture startLocalListening() {
		ServerBootstrap b = new ServerBootstrap();
		b.group(group)
		 .channel(NioServerSocketChannel.class)
		 .childHandler(new ChannelInitializer<SocketChannel>() {
			 @Override
			 public void initChannel(SocketChannel ch) throws Exception {
				 if (!acceptPolicy.acceptingConnection(new NettyNewConnectionContext(ch))) {
					 ch.close();
					 return;
				 }

				 allChannels.add(ch);
				 NettyUtils.setupPipeline(ch, KryoBuilder.build(params.getMessageTypes()), params.getListenerFactory(), connectionStateListener);
			 }
		 })
		 .childOption(ChannelOption.TCP_NODELAY, true)
		 .childOption(ChannelOption.SO_KEEPALIVE, true);

		listeningFuture.thenRun(() -> log.info("Server listening on {}", params.getLocalAddress()));
		ChannelFuture future = b.bind(params.getLocalAddress());
		future.addListener(f -> {
			if (f.isSuccess())
				listeningFuture.complete(null);
			else
				listeningFuture.completeExceptionally(f.cause());
		});
		return future;
	}

	public ChannelFuture startRemoteListening() {
		Bootstrap b = new Bootstrap();
		b.group(group)
		 .channel(NioSocketChannel.class)
		 .option(ChannelOption.TCP_NODELAY, true)
		 .handler(new ChannelInitializer<SocketChannel>() {
			 @Override
			 public void initChannel(SocketChannel ch) throws Exception {
				 if (params.getProtocol() == Protocol.WS)
					 NettyUtils.configurePipelineForWebsocketClient(ch, params);

				 ch.pipeline()
				   .addLast(new LineBasedFrameDecoder(512, true, true))
				   .addLast(new ProxyRemoteServerHandler(params.getProxyTag(), NettyServer.this::addClient, listeningFuture));
			 }
		 });

		listeningFuture.thenRun(() -> log.info("Remotely listening on service:{} at {}", params.getProxyTag(), params.getRemoteAddress()));
		return b.connect(params.getRemoteAddress(), params.getLocalAddress());
	}

	@Override
	public void stopAcceptingNewConnections() {
		serverChannel.close();
	}

	@Override
	public void close() {
		if (allChannels != null) {
			allChannels.close();
			allChannels = null;
		}
		if (group != null) {
			group.shutdownGracefully();
			group = null;
		}
	}

	private void addClient(String tag) {
		log.info("Received incoming connection with tag {}", tag);

		Kryo kryo = KryoBuilder.build(params.getMessageTypes());

		Bootstrap b = new Bootstrap();
		b.group(group)
		 .channel(NioSocketChannel.class)
		 .option(ChannelOption.TCP_NODELAY, true)
		 .handler(new ChannelInitializer<SocketChannel>() {
			 @Override
			 public void initChannel(SocketChannel ch) throws Exception {
				 if (params.getProtocol() == Protocol.WS)
					 NettyUtils.configurePipelineForWebsocketClient(ch, params);

				 NettyUtils.setupClientPipeline(ch, tag, kryo, params, connectionStateListener);
			 }
		 });

		// Start the client.
		b.connect(params.getRemoteAddress()); //TODO: handle errors?
	}
}
