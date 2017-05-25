package org.gillius.jagnet.netty;

import com.esotericsoftware.kryo.Kryo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.gillius.jagnet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class NettyClient implements Client {
	private static final Logger log = LoggerFactory.getLogger(NettyClient.class);

	private final ConnectionParams params;

	private NettyConnection connection;
	private final CompletableFuture<Connection> connFuture = new CompletableFuture<>();
	private EventLoopGroup group;

	public NettyClient(ConnectionParams params) {
		this.params = params.clone();
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

	public void start() {
		//TODO: prevent starting more than once (race conditions on close/failure)

		Kryo kryo = KryoBuilder.build(params.getMessageTypes());

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
				 if (params.getProtocol() == Protocol.WS)
					 NettyUtils.configurePipelineForWebsocketClient(ch, params);

				 NettyUtils.setupClientPipeline(ch, params.getProxyTag(), kryo, params, getConnectionStateListener());
			 }
		 });

		// Start the client.
		b.connect(params.getRemoteAddress(), params.getLocalAddress()).addListener(future -> {
			if (!future.isSuccess()) {
				connFuture.completeExceptionally(future.cause());
				close();
			}
		});
	}

	@Override
	public CompletableFuture<Connection> getConnection() {
		return connFuture;
	}

	private ConnectionStateListener getConnectionStateListener() {
		return new ConnectionStateListener() {
			@Override
			public void onConnected(ConnectionListenerContext ctx) {
				connFuture.complete(connection);
			}
		};
	}
}
