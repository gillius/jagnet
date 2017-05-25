package org.gillius.jagnet.netty;

import com.esotericsoftware.kryo.Kryo;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import org.gillius.jagnet.ConnectionListener;
import org.gillius.jagnet.ConnectionParams;
import org.gillius.jagnet.ConnectionStateListener;
import org.gillius.jagnet.NewConnectionContext;
import org.gillius.jagnet.proxy.client.ProxyClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

/**
 * Utility functions for the netty-based jagnet implementation.
 */
class NettyUtils {
	private static final Logger log = LoggerFactory.getLogger(NettyUtils.class);

	public static void configurePipelineForWebsocketClient(SocketChannel ch, ConnectionParams params) throws URISyntaxException {
		URI uri = new URI("ws", null,
		                  params.getRemoteAddress().getHostString(), params.getRemoteAddress().getPort(),
		                  params.getWebsocketPath(), null, null);
		ch.pipeline()
		  .addLast(new HttpClientCodec())
		  .addLast(new HttpObjectAggregator(8192))
		  .addLast(new WebsocketClientHandler(uri));
	}

	public static void setupClientPipeline(SocketChannel ch, String tag, Kryo kryo, ConnectionParams params, ConnectionStateListener connectionStateListener) {
		if (tag != null) {
			setupProxyClientPipeline(ch, tag, kryo, params, connectionStateListener);
		} else {
			setupPipeline(ch, kryo, params.getListenerFactory(), connectionStateListener);
		}
	}

	public static void setupProxyClientPipeline(SocketChannel ch, String tag, Kryo kryo, ConnectionParams params, ConnectionStateListener connectionStateListener) {
		ch.pipeline()
		  .addLast(new LineBasedFrameDecoder(512, true, true))
		  .addLast(new ProxyClientHandler(tag, ctx -> {
			  log.info("Switching from proxy mode");
			  ChannelPipeline p = ctx.pipeline();
			  p.remove(LineBasedFrameDecoder.class);
			  p.remove(ProxyClientHandler.class);
			  setupPipeline(ch, kryo, params.getListenerFactory(), connectionStateListener);
			  ch.pipeline().fireChannelActive();
		  }));
	}

	public static void setupPipeline(SocketChannel ch, Kryo kryo,
	                                 Function<NewConnectionContext, ConnectionListener> listenerFactory,
	                                 ConnectionStateListener connectionStateListener) {
		ch.pipeline()
		  .addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2))
		  .addLast(new KryoDecoder(kryo))
		  .addLast(new KryoEncoder(kryo, true))
		  .addLast(new NettyHandler(new NettyConnection(ch), listenerFactory.apply(new NettyNewConnectionContext(ch)), connectionStateListener));
	}
}
