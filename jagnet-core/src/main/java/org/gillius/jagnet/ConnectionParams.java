package org.gillius.jagnet;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.function.Function;

public class ConnectionParams implements Cloneable {
	private InetSocketAddress remoteAddress;
	private Protocol protocol;
	private String proxyTag;
	private String websocketPath;
	private LinkedHashSet<Class<?>> messageTypes = new LinkedHashSet<>();
	private Function<NewConnectionContext, ConnectionListener> listenerFactory = null;

	@Override
	public ConnectionParams clone() {
		try {
			ConnectionParams ret = (ConnectionParams) super.clone();
			ret.messageTypes = new LinkedHashSet<>(messageTypes);
			return ret;

		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); //should not be possible
		}
	}

	/**
	 * Calls {@link #setByURI(URI)} with the given string parsed as an URI.
	 */
	public ConnectionParams setByURI(String uri) throws URISyntaxException {
		return setByURI(new URI(uri));
	}

	/**
	 * Assigns protocol, proxyTag, remoteAddress, and websocketPath from URI. The URI takes the form of:
	 * protocol://host:port/websocketPath?proxyTag. The protocol is one of "proxy+tcp", "proxy+ws", "tcp", or "ws". The
	 * proxyTag is observed only if scheme is one of the proxy schemes, and websocketPath observed only if one of the
	 * websocket (ws) schemes, else those values are set to null.
	 * <p>
	 * If the port is not specified, it defaults to 80 for ws protocol, or 56238 for TCP proxy mode, or 54555 for TCP
	 * not proxy mode.
	 */
	public ConnectionParams setByURI(URI uri) {
		//Wait to assign in case exception occurs later

		Protocol protocol;
		String proxyTag;
		int port = uri.getPort();
		switch (uri.getScheme().toLowerCase()) {
			case "proxy+tcp":
				protocol = Protocol.TCP;
				proxyTag = uri.getQuery();
				break;

			case "proxy+ws":
				protocol = Protocol.WS;
				proxyTag = uri.getQuery();
				break;

			case "tcp":
				protocol = Protocol.TCP;
				proxyTag = null;
				break;

			case "ws":
				protocol = Protocol.WS;
				proxyTag = null;
				break;

			default:
				throw new IllegalArgumentException("Unknown scheme " + uri.getScheme());
		}

		if (port < 0) {
			port = protocol == Protocol.TCP ? (proxyTag != null ? 56238 : 54555): 80;
		}

		this.remoteAddress = new InetSocketAddress(uri.getHost(), port);
		this.protocol = protocol;
		this.proxyTag = proxyTag;

		if (protocol == Protocol.WS)
			websocketPath = uri.getPath();
		else
			websocketPath = null;

		return this;
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public ConnectionParams setRemoteAddress(InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
		return this;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public ConnectionParams setProtocol(Protocol protocol) {
		this.protocol = protocol;
		return this;
	}

	public boolean isProxyMode() {
		return proxyTag != null;
	}

	public String getProxyTag() {
		return proxyTag;
	}

	public ConnectionParams setProxyTag(String proxyTag) {
		this.proxyTag = proxyTag;
		return this;
	}

	public String getWebsocketPath() {
		return websocketPath;
	}

	public ConnectionParams setWebsocketPath(String websocketPath) {
		this.websocketPath = websocketPath;
		return this;
	}

	public LinkedHashSet<Class<?>> getMessageTypes() {
		return messageTypes;
	}

	public ConnectionParams registerMessages(Iterable<Class<?>> messageTypes) {
		for (Class<?> messageType : messageTypes) {
			this.messageTypes.add(messageType);
		}
		return this;
	}

	public ConnectionParams registerMessages(Class<?>... messageTypes) {
		Collections.addAll(this.messageTypes, messageTypes);
		return this;
	}

	/**
	 * A shortcut for calling {@link #setListenerFactory(Function)} with a factory always returning this listener.
	 */
	public ConnectionParams setListener(ConnectionListener listener) {
		setListenerFactory(x -> listener);
		return this;
	}

	public Function<NewConnectionContext, ConnectionListener> getListenerFactory() {
		return listenerFactory;
	}

	public ConnectionParams setListenerFactory(Function<NewConnectionContext, ConnectionListener> listenerFactory) {
		this.listenerFactory = listenerFactory;
		return this;
	}
}
