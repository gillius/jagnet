package org.gillius.jagnet.examples;

import org.gillius.jagnet.*;
import org.gillius.jagnet.netty.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerExample {
	private static final Logger log = LoggerFactory.getLogger(ServerExample.class);

	public static void main(String[] args) throws Exception {
		NettyServer remote = new NettyServer();

		ConnectionParams params = new ConnectionParams()
				.setByURI("tcp://localhost:54555", true);
//				.setByURI("proxy+ws://localhost:56238/websocket?ServerExample", true);

		remote.setConnectionStateListener(new ConnectionStateListener() {
			@Override
			public void onConnected(ConnectionListenerContext ctx) {
				log.info("Received connection from " + ctx.getConnection().getRemoteAddress());
			}

			@Override
			public void onDisconnected(ConnectionListenerContext ctx) {
				log.info("Disconnect from " + ctx.getConnection().getRemoteAddress());
			}
		});

		KryoCopier copier = new KryoCopier().register(FrameworkMessages.getMessageTypes());

		ObjectManager objectManager = new ObjectManager();
		objectManager.setOnCreateListener((it) -> log.info("{}", it));
		objectManager.setOnUpdateListener(m -> {
			copier.copy(m.getMessage(), m.getRegisteredObject());
			log.info("Updated: {}", m.getRegisteredObject());
		});

		objectManager.registerObject(1, new ChatMessage("Server", "original"));

		params.setListener(ConnectionListenerChain.of(new StandardConnectionListener(),
		                                              new ObjectManagerConnectionListener(objectManager)));
		remote.start(params);
	}
}
