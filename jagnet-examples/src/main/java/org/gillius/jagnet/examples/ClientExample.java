package org.gillius.jagnet.examples;

import org.gillius.jagnet.*;
import org.gillius.jagnet.netty.NettyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gillius.jagnet.examples.ExampleUtil.getUri;

public class ClientExample {
	private static final Logger log = LoggerFactory.getLogger(ClientExample.class);

	public static void main(String[] args) throws Exception {
		ConditionConnectionListener listener = new ConditionConnectionListener();

		ConnectionParams params = new ConnectionParams()
				.setByURI(getUri(args), false)
				.setListener(listener);

		NettyClient client = new NettyClient(params);
		TimeSync sync = new TimeSync();

		client.start();
		Connection conn = client.getConnection().get();

		for (int i=0; i<3; ++i) {
			//TODO: currently we "sendReliable" but the standard listener always replies with "sendFast"
			TimeSyncResponse response = listener.sendReliableAndReceive(sync.send(), TimeSyncResponse.class).join();
			sync.receive(response);
			log.info("RTT: {}", sync.getRtt());
			log.info("Offset: {}", sync.getTimeOffset());
		}
		conn.sendReliable(new ChatMessage("Jason", "Hello"));
		conn.sendReliable(new ObjectCreateMessage<>(0, new ChatMessage("Object", "Create")));
		conn.sendReliable(new ObjectUpdateMessage<>(1, new ChatMessage("Object", "Create")));

		conn.close();
		conn.getCloseFuture().join(); //wait for the connection to actually close
	}
}
