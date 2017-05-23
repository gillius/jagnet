package org.gillius.jagnet.netty;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import org.gillius.jagnet.ConnectionParams;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility functions for the netty-based jagnet implementation.
 */
class NettyUtils {
	public static void configurePipelineForWebsocketClient(SocketChannel ch, ConnectionParams params) throws URISyntaxException {
		URI uri = new URI("ws", null,
		                  params.getRemoteAddress().getHostString(), params.getRemoteAddress().getPort(),
		                  params.getWebsocketPath(), null, null);
		ch.pipeline()
		  .addLast(new HttpClientCodec())
		  .addLast(new HttpObjectAggregator(8192))
		  .addLast(new WebsocketClientHandler(uri));
	}
}
