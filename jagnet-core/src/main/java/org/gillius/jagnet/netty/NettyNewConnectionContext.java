package org.gillius.jagnet.netty;

import io.netty.channel.socket.SocketChannel;
import org.gillius.jagnet.NewConnectionContext;

import java.net.InetSocketAddress;

public class NettyNewConnectionContext implements NewConnectionContext {
	private final SocketChannel channel;

	public NettyNewConnectionContext(SocketChannel channel) {
		this.channel = channel;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return channel.localAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return channel.remoteAddress();
	}
}
