package org.gillius.jagnet.kryonet;

import com.esotericsoftware.kryonet.EndPoint;
import org.gillius.jagnet.ConnectionListener;
import org.gillius.jagnet.FrameworkMessages;
import org.gillius.jagnet.Server;

import java.io.IOException;

public class KryonetServer extends KryonetClientServerBase implements Server {
	private com.esotericsoftware.kryonet.Server kryonetServer = new com.esotericsoftware.kryonet.Server();

	public KryonetServer() {
		FrameworkMessages.register(kryonetServer.getKryo());
	}

	@Override
	protected EndPoint getEndPoint() {
		return kryonetServer;
	}

	@Override
	public void setListener(ConnectionListener listener) {
		//TODO: switch to builder pattern so we can only get one listener
		kryonetServer.addListener(new KryonetConnectionListenerAdapter(listener));
	}

	@Override
	public void start(boolean daemon) throws IOException {
		Thread thread = new Thread(this.kryonetServer, "KryonetServer");
		thread.setDaemon(daemon);
		thread.start();
		this.kryonetServer.bind(port, port);
	}
}
