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
import org.gillius.jagnet.Connection;
import org.gillius.jagnet.ConnectionListener;
import org.gillius.jagnet.NoopConnectionListener;
import org.gillius.jagnet.Server;

import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

public class NettyServer implements Server {
	private int port = -1;
	private final KryoBuilder kryoBuilder = new KryoBuilder();
	private ConnectionListener listener = NoopConnectionListener.INSTANCE;

	private final BlockingQueue<CompletableFuture<Connection>> unconnectedSlots;
	private final BlockingQueue<CompletableFuture<Connection>> slots;

	private ChannelGroup allChannels;
	private EventLoopGroup group;
	private Channel serverChannel; //TODO: close listener when slots are full?

	/**
	 * Create a server with no limit on the number of connections. This means {@link #getConnection()} will always throw,
	 * so connections are received solely through the listener.
	 */
	public NettyServer() {
		unconnectedSlots = null;
		slots = null;
	}

	/**
	 * Creates a server with a fixed number of connection slots. Each call to {@link #getConnection()} will return the
	 * next slot until none remain.
	 */
	public NettyServer(int numSlots) {
		unconnectedSlots = new ArrayBlockingQueue<>(numSlots);
		slots = new ArrayBlockingQueue<>(numSlots);
		for (int i=0; i < numSlots; ++i) {
			CompletableFuture<Connection> f = new CompletableFuture<>();
			unconnectedSlots.add(f);
			slots.add(f);
		}
	}

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
	public void setListener(ConnectionListener listener) {
		this.listener = listener;
	}

	@Override
	public void start() {
		group = new NioEventLoopGroup(1);
		allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		ServerBootstrap b = new ServerBootstrap();
		b.group(group)
		 .channel(NioServerSocketChannel.class)
		 .childHandler(new ChannelInitializer<SocketChannel>() {
			 @Override
			 public void initChannel(SocketChannel ch) throws Exception {
				 if (!listener.acceptingConnection(ch.remoteAddress())) {
					 ch.close();
					 return;
				 }

				 CompletableFuture<Connection> connFuture;
				 if (unconnectedSlots != null) {
					 connFuture = unconnectedSlots.poll();
					 if (connFuture == null) {
						 ch.close();
						 return;
					 }
				 } else {
				 	connFuture = new CompletableFuture<>();
				 }

				 allChannels.add(ch);
				 setupPipeline(ch, connFuture);
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

	public void stopAcceptingNewConnections() {
		serverChannel.close();
	}

	@Override
	public void close() {
		if (allChannels != null) {
			try {
				allChannels.close().await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			allChannels = null;
		}
		if (group != null)
			group.shutdownGracefully();
		group = null;
	}

	@Override
	public CompletableFuture<Connection> getConnection() {
		CompletableFuture<Connection> ret = slots.poll();
		if (ret == null)
			throw new NoSuchElementException("No slots remaining");
		return ret;
	}

	protected ChannelPipeline setupPipeline(SocketChannel ch, CompletableFuture<Connection> connFuture) {
		return ch.pipeline()
		         .addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2))
		         .addLast(new KryoDecoder(kryoBuilder.get()))
		         .addLast(new KryoEncoder(kryoBuilder.get()))
		         //TODO: setListener after opening connection?
		         .addLast(new NettyHandler(new NettyConnection(ch), listener, connFuture));
	}
}
