package org.gillius.jagnet.examples;

import org.gillius.jagnet.*;
import org.gillius.jagnet.netty.NettyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientExample {
	private static final Logger log = LoggerFactory.getLogger(ClientExample.class);

	public static void main(String[] args) throws Exception {
		NettyClient client = new NettyClient();
		client.setPort(54555);
		client.setHost("localhost");
//		client.setProxyTag("thetag");

		TimeSync sync = new TimeSync();
		AtomicInteger count = new AtomicInteger(0);
		CompletableFuture<Object> timeSynced = new CompletableFuture<>();

		ConnectionListener listener = new ConnectionListener() {
			@Override
			public void onReceive(ConnectionListenerContext ctx, Object message) {
				if (message instanceof TimeSyncResponse) {
					TimeSyncResponse response = (TimeSyncResponse) message;
					sync.receive(response);
					log.info("RTT: {}", sync.getRtt());
					log.info("Offset: {}", sync.getTimeOffset());
					if (count.incrementAndGet() < 3)
						sync.send(ctx.getConnection());
					else
						timeSynced.complete(null);
				}
			}
		};
		client.setListener(listener);

		client.start();
		Connection conn = client.getConnection().get();
		sync.send(conn);
		timeSynced.get();
		conn.sendReliable(new ChatMessage("Jason", "Hello"));
		conn.sendReliable(new ObjectCreateMessage<>(0, new ChatMessage("Object", "Create")));
		conn.sendReliable(new ObjectUpdateMessage<>(1, new ChatMessage("Object", "Create")));

		conn.close();
	}
}
