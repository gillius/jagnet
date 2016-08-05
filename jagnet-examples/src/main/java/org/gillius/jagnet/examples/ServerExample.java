package org.gillius.jagnet.examples;

import org.gillius.jagnet.*;
import org.gillius.jagnet.netty.NettyClient;
import org.gillius.jagnet.netty.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerExample {
	private static final Logger log = LoggerFactory.getLogger(ServerExample.class);

	public static void main(String[] args) throws Exception {
		Server remote = new NettyServer();
		remote.setPort(54555);
//		NettyClient remote = new NettyClient();
//		remote.setHost("localhost");
//		remote.setPort(8000);
//		remote.setProxyTag("thetag");

		KryoCopier copier = new KryoCopier().register(FrameworkMessages.getMessageTypes());

		ObjectManager objectManager = new ObjectManager();
		objectManager.setOnCreateListener((it) -> log.info("{}", it));
		objectManager.setOnUpdateListener(m -> {
			copier.copy(m.getMessage(), m.getRegisteredObject());
			log.info("Updated: {}", m.getRegisteredObject());
		});

		objectManager.registerObject(1, new ChatMessage("Server", "original"));

		remote.setListener(
				new StandardConnectionListener(
						new ObjectManagerConnectionListener(objectManager)));

		remote.start();
	}
}
