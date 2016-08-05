package org.gillius.jagnet.examples;

import com.esotericsoftware.kryo.Kryo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import org.gillius.jagnet.netty.KryoDecoder;
import org.gillius.jagnet.netty.KryoEncoder;
import org.gillius.jagnet.proxy.client.ProxyClientHandler;

public class ProxyClient {
	public static void main(String[] args) throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group)
			 .channel(NioSocketChannel.class)
			 .option(ChannelOption.TCP_NODELAY, true)
			 .handler(new ChannelInitializer<SocketChannel>() {
				 @Override
				 public void initChannel(SocketChannel ch) throws Exception {
					 ch.pipeline()
					   .addLast(new LineBasedFrameDecoder(512, true, true))
					   .addLast(new ProxyClientHandler("thetag", ctx -> {
						   ChannelPipeline p = ctx.pipeline();
						   p.remove(LineBasedFrameDecoder.class);
						   p.remove(ProxyClientHandler.class);
						   p.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
						   p.addLast(new KryoDecoder(makeKryo()));
						   p.addLast(new KryoEncoder(makeKryo()));
						   p.addLast(new LoggingHandler());

						   ch.writeAndFlush(new Message("I'm a message!"));
					   }));
				 }
			 });

			// Start the client.
			ChannelFuture f = b.connect("localhost", 56238).sync();

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
