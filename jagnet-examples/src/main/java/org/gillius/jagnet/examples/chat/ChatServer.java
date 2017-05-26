package org.gillius.jagnet.examples.chat;

import org.gillius.jagnet.*;
import org.gillius.jagnet.netty.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.gillius.jagnet.FilteredReceivedMessageListener.typedListener;
import static org.gillius.jagnet.examples.ExampleUtil.getUri;

public class ChatServer {
	public static final List<Class<?>> MESSAGE_CLASSES = Arrays.asList(ChatRegistration.class, ClientChatMessage.class);

	private static final Logger log = LoggerFactory.getLogger(ChatServer.class);

	public static void main(String[] args) throws Exception {
		Server server = new NettyServer();

		ChatRoom room = new ChatRoom();
		ConnectionParams params = new ConnectionParams()
				.setByURI(getUri(args), true)
				.registerMessages(MESSAGE_CLASSES)
				.setListenerFactory(x -> new RemoteChatClient(room));
		server.start(params);

		log.info("Server listening on port 54555");
	}

	private static class ChatRoom {
		private Map<Connection, String> activeUsers = new IdentityHashMap<>();

		public void onUserJoin(Connection conn, String name) {
			activeUsers.put(conn, name);
			sendMessage("Server", name + " from " + conn.getRemoteAddress() + " has entered the chat");
		}

		public void onUserLeave(Connection conn) {
			activeUsers.remove(conn);
			sendMessage("Server", getName(conn) + " from " + conn.getRemoteAddress() + " has left the chat");
		}

		public void onClientChat(Connection conn, ClientChatMessage message) {
			sendMessage(getName(conn), message.message);
		}

		private String getName(Connection conn) {
			return activeUsers.getOrDefault(conn, "Unknown User!");
		}

		public void sendMessage(String name, String message) {
			ChatMessage toSend = new ChatMessage(name, message);
			System.out.format("%20s: %s%n", toSend.name, toSend.message);
			for (Connection c : activeUsers.keySet()) {
				c.sendReliable(toSend);
			}
		}
	}

	private static class RemoteChatClient implements ConnectionListener {
		private final ChatRoom room;

		private ReceivedMessageListener state = typedListener(ChatRegistration.class, this::registrationStateReceive);

		public RemoteChatClient(ChatRoom room) {
			this.room = room;
		}

		@Override
		public void onDisconnected(ConnectionListenerContext ctx) {
			room.onUserLeave(ctx.getConnection());
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onReceive(ConnectionListenerContext ctx, Object message) {
			state.onReceive(ctx, message);
		}

		private void registrationStateReceive(ConnectionListenerContext ctx, ChatRegistration registration) {
			if (registration.name == null || registration.name.isEmpty()) {
				ctx.getConnection().close();
			} else {
				room.onUserJoin(ctx.getConnection(), registration.name);
				state = typedListener(ClientChatMessage.class, this::activeStateReceive);
			}
		}

		private void activeStateReceive(ConnectionListenerContext ctx, ClientChatMessage chatMessage) {
			room.onClientChat(ctx.getConnection(), chatMessage);
		}
	}
}
