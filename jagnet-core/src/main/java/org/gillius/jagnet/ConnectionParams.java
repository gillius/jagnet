package org.gillius.jagnet;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.function.Function;

public class ConnectionParams implements Cloneable {
	private InetSocketAddress localAddress;
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
	 * Calls {@link #setByURI(URI, boolean)} with the given string parsed as an URI.
	 */
	public ConnectionParams setByURI(String uri, boolean server) throws URISyntaxException {
		return setByURI(new URI(uri), server);
	}

	/**
	 * Assigns protocol, proxyTag, remoteAddress (or localAddress), and websocketPath from URI. The URI takes the form of:
	 * protocol://host:port/websocketPath?proxyTag. The protocol is one of "proxy+tcp", "proxy+ws", "tcp", or "ws". The
	 * proxyTag is observed only if scheme is one of the proxy schemes, and websocketPath observed only if one of the
	 * websocket (ws) schemes, else those values are set to null.
	 * <p>
	 * The server parameter determines if this is to be considered a "server" URI. In a server URI, the proxy type
	 * protocols assume a remote proxy connection and set remoteAddress. The other protocols assume a local connection
	 * and set localAddress.
	 * <p>
	 * Host value "any" is treated as the wildcard "0.0.0.0" address, which is useful when setting localAddress when
	 * server parameter is true.
	 * <p>
	 * If the port is not specified, it defaults to 80 for ws protocol, or 56238 for TCP proxy mode, or 54555 for TCP
	 * not proxy mode.
	 */
	public ConnectionParams setByURI(URI uri, boolean server) {
		//Wait to assign in case exception occurs later

		Protocol protocol;
		String proxyTag;
		int port = uri.getPort();
		switch (uri.getScheme().toLowerCase()) {
			case "proxy+tcp":
				protocol = Protocol.TCP;
				proxyTag = getProxyTagFromURI(uri);
				break;

			case "proxy+ws":
				protocol = Protocol.WS;
				proxyTag = getProxyTagFromURI(uri);
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

		String host = uri.getHost();
		if ("any".equalsIgnoreCase(host))
			host = "0.0.0.0";
		InetSocketAddress addr = new InetSocketAddress(host, port);
		if (server) {
			if (proxyTag != null)
				remoteAddress = addr;
			else
				localAddress = addr;
		} else {
			remoteAddress = addr;
		}
		this.protocol = protocol;
		this.proxyTag = proxyTag;

		if (protocol == Protocol.WS)
			websocketPath = uri.getPath();
		else
			websocketPath = null;

		return this;
	}

	private static String getProxyTagFromURI(URI uri) {
		if (uri.getQuery() == null || uri.getQuery().isEmpty())
			throw new IllegalArgumentException("proxy type protocol specified but proxyTag not specified");
		return uri.getQuery();
	}

	public InetSocketAddress getLocalAddress() {
		return localAddress;
	}

	public ConnectionParams setLocalAddress(InetSocketAddress localAddress) {
		this.localAddress = localAddress;
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

	/**
	 * A function that given the incoming connection context, returns a {@link ConnectionListener} to use for that
	 * connection. Since a listener for a given connection is not called concurrently, this allows the function to return
	 * connection-specific listener instances if they are stateful. Or, the factory could return the same value each time
	 * if the listener is stateless or thread-safe.
	 */
	public ConnectionParams setListenerFactory(Function<NewConnectionContext, ConnectionListener> listenerFactory) {
		this.listenerFactory = listenerFactory;
		return this;
	}
}
