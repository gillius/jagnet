package org.gillius.jagnet.examples.chat;

import org.gillius.jagnet.*;
import org.gillius.jagnet.netty.NettyClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.gillius.jagnet.examples.ExampleUtil.getUri;

public class ChatClient {
	public static void main(String[] args) throws Exception {
		ConnectionParams params = new ConnectionParams()
				.setByURI(getUri(args), false)
				.registerMessages(ChatServer.MESSAGE_CLASSES);

		params.setListener(new TypedConnectionListener()
				                   .setListener(ChatMessage.class, (ctx, chatMessage) ->
						                   System.out.format("%20s: %s%n", chatMessage.name, chatMessage.message)));

		NettyClient client = new NettyClient(params);
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
