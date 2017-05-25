package org.gillius.jagnet.netty;

import com.esotericsoftware.kryo.Kryo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.gillius.jagnet.*;
import org.gillius.jagnet.proxy.ProxyRemoteServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class NettyRemoteServer {
	private static final Logger log = LoggerFactory.getLogger(NettyRemoteServer.class);

	private ConnectionParams params;
	private ConnectionStateListener connectionStateListener = NoopConnectionListener.INSTANCE;
	private final CompletableFuture<Object> listeningFuture = new CompletableFuture<>();

	private ChannelGroup allChannels;
	private EventLoopGroup group;
	private Channel serverChannel;

	public AcceptPolicy getAcceptPolicy() {
		return AcceptAllPolicy.INSTANCE;
	}

	public void setAcceptPolicy(AcceptPolicy acceptPolicy) {
		throw new UnsupportedOperationException("Not supported for remote server connections");
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
		if (params.getRemoteAddress() == null)
			throw new IllegalArgumentException("remoteAddress not initialized");
		if (params.getProxyTag() == null)
			throw new IllegalArgumentException("proxyTag not initialized");

		this.params = params.clone();

		group = new NioEventLoopGroup(1);
		allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

		ChannelFuture future = startRemoteListening();

		serverChannel = future.channel();
		allChannels.add(serverChannel);
		try {
			future.sync(); //sync to wait here for bind to complete or connect to start, so we get exception if it fails
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
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
				   .addLast(new ProxyRemoteServerHandler(params.getProxyTag(), NettyRemoteServer.this::addClient, listeningFuture));
			 }
		 });

		return b.connect(params.getRemoteAddress(), params.getLocalAddress());
	}

//	@Override
	public void stopAcceptingNewConnections() {
		serverChannel.close();
	}

//	@Override
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
