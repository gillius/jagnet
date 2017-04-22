package org.gillius.jagnet.examples.chat;

import org.gillius.jagnet.ChatMessage;
import org.gillius.jagnet.Connection;
import org.gillius.jagnet.ConnectionListener;
import org.gillius.jagnet.ConnectionListenerContext;
import org.gillius.jagnet.netty.NettyClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ChatClient {
	public static void main(String[] args) {
		NettyClient client = new NettyClient();
		client.setPort(54555);
		client.setHost("localhost");
		client.registerMessages(ChatServer.MESSAGE_CLASSES);

		client.setListener(new ConnectionListener() {
			@Override
			public void onReceive(ConnectionListenerContext ctx, Object message) {
				if (message instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage) message;
					System.out.format("%20s: %s%n", chatMessage.name, chatMessage.message);
				}
			}
		});

		client.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Connecting to server...");

		try (Connection conn = client.getConnection().join()) {
			System.out.println("Connected.");
			System.out.print("Enter name: ");
			String name = reader.readLine();
			conn.sendReliable(new ChatRegistration(name));

			String line;
			boolean done = false;
			while (!done) {
				line = reader.readLine();
				if (line != null && !"exit".equals(line))
					conn.sendReliable(new ClientChatMessage(line));
				else
					done = true;
			}
		} catch (IOException e) {
			//just exit if stdin ends
		}
	}
}
