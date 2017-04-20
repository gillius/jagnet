package org.gillius.jagnet.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.gillius.jagnet.*;

import java.util.function.Function;

public class NettyServer implements Server {
	private int port = -1;
	private final KryoBuilder kryoBuilder = new KryoBuilder();
	private AcceptPolicy acceptPolicy = AcceptAllPolicy.INSTANCE;
	private Function<NewConnectionContext, ConnectionListener> listenerFactory = null;
	private ConnectionStateListener connectionStateListener = NoopConnectionListener.INSTANCE;

	private ChannelGroup allChannels;
	private EventLoopGroup group;
	private Channel serverChannel;

	@Override
	public void setPort(int port) {
		this.port = port;
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
	public void setListenerFactory(Function<NewConnectionContext, ConnectionListener> factory) {
		this.listenerFactory = factory;
	}

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

	@Override
	public ConnectionStateListener getConnectionStateListener() {
		return connectionStateListener;
	}

	@Override
	public void setConnectionStateListener(ConnectionStateListener connectionStateListener) {
		this.connectionStateListener = connectionStateListener;
	}

	@Override
	public void start() {
		if (listenerFactory == null)
			throw new IllegalStateException("listenerFactory not initialized");
		if (port < 0)
			throw new IllegalStateException("port not initialized");

		group = new NioEventLoopGroup(1);
		allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
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
				 setupPipeline(ch);
			 }
		 })
		 .childOption(ChannelOption.TCP_NODELAY, true)
		 .childOption(ChannelOption.SO_KEEPALIVE, true);

		ChannelFuture future = b.bind(port);
		serverChannel = future.channel();
		allChannels.add(serverChannel);
		try {
			future.sync(); //sync to wait here for bind to complete, so we get exception if it fails
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
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

	protected void setupPipeline(SocketChannel ch) {
		ch.pipeline()
		  .addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2))
		  .addLast(new KryoDecoder(kryoBuilder.get()))
		  .addLast(new KryoEncoder(kryoBuilder.get()))
		  .addLast(new NettyHandler(new NettyConnection(ch), listenerFactory.apply(new NettyNewConnectionContext(ch)), connectionStateListener));
	}
}
