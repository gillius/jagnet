package org.gillius.jagnet.kryonet;

import com.esotericsoftware.kryonet.EndPoint;
import org.gillius.jagnet.Client;
import org.gillius.jagnet.Connection;
import org.gillius.jagnet.ConnectionListener;
import org.gillius.jagnet.FrameworkMessages;

import java.io.IOException;

public class KryonetClient extends KryonetClientServerBase implements Client {
	private final com.esotericsoftware.kryonet.Client kryonetClient = new com.esotericsoftware.kryonet.Client();
	private final Connection connection = new KryonetConnectionAdapter(kryonetClient);
	private String host = null;

	public KryonetClient() {
		FrameworkMessages.register(kryonetClient.getKryo());
	}

	protected EndPoint getEndPoint() {
		return kryonetClient;
	}

	@Override
	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public void setListener(ConnectionListener listener) {
		//TODO: switch to builder pattern so we can only get one listener
		kryonetClient.addListener(new KryonetClientConnectionListenerAdapter(listener, connection));
	}

	@Override
	public void connect() throws IOException {
		kryonetClient.start();
		kryonetClient.connect(5000, host, port, port);
	}

	@Override
	public void sendReliable(Object message) {
		connection.sendReliable(message);
	}

	@Override
	public void sendFast(Object message) {
		connection.sendFast(message);
	}

}
