package org.gillius.jagnet.examples;

import com.esotericsoftware.kryo.Kryo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import org.gillius.jagnet.netty.KryoDecoder;
import org.gillius.jagnet.netty.KryoEncoder;
import org.gillius.jagnet.netty.WebsocketClientHandler;
import org.gillius.jagnet.proxy.client.ProxyClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ProxyClient {
	private static final Logger log = LoggerFactory.getLogger(ProxyClient.class);

	private static boolean isEmpty(String s) {
		return s == null || s.isEmpty();
	}

	private static boolean isWebsocket(URI destination) {
		switch (destination.getScheme()) {
			case "tcp":
				return false;
			case "ws":
				return true;
			default:
				System.err.println("scheme must be tcp or ws in " + destination);
				System.exit(1);
				return false;
		}
	}

	public static void main(String[] args) throws Exception {
		final URI destination;
		if (args.length > 0)
			destination = new URI(args[0]);
		else
			destination = new URI("tcp://localhost:56238/");

		if (isEmpty(destination.getHost())) {
			System.err.println("Host is undefined in " + destination);
			System.exit(1);
		}
		if (destination.getPort() == -1) {
			System.err.println("Port is undefined in " + destination);
			System.exit(1);
		}
		boolean websocket = isWebsocket(destination);

		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group)
			 .channel(NioSocketChannel.class)
			 .option(ChannelOption.TCP_NODELAY, true)
			 .handler(new ChannelInitializer<SocketChannel>() {
				 @Override
				 public void initChannel(SocketChannel ch) throws Exception {
					 if (websocket) {
						 log.info("Running in websocket mode");
						 ch.pipeline()
						   .addLast(new HttpClientCodec())
						   .addLast(new HttpObjectAggregator(8192))
						   .addLast(new WebsocketClientHandler(destination));
					 }
					 ch.pipeline()
					   .addLast(new LineBasedFrameDecoder(512, true, true))
					   .addLast(new ProxyClientHandler("thetag", ctx -> {
						   ChannelPipeline p = ctx.pipeline();
						   p.remove(LineBasedFrameDecoder.class);
						   p.remove(ProxyClientHandler.class);
						   if (!websocket)
							   p.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
						   p.addLast(new KryoDecoder(makeKryo()));
						   p.addLast(new KryoEncoder(makeKryo(), !websocket));
						   p.addLast(new LoggingHandler());

						   Runnable command = () -> ch.writeAndFlush(new Message("Hello it is " + new Date()));

						   ch.eventLoop().scheduleAtFixedRate(command, 0, 2, TimeUnit.SECONDS);
					   }));
				 }
			 });

			// Start the client.
			ChannelFuture f = b.connect(destination.getHost(), destination.getPort()).sync();

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

}
