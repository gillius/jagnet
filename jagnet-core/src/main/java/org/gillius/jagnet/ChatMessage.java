package org.gillius.jagnet;

public class ChatMessage {
	public String name;
	public String message;

	public ChatMessage() {
	}

	public ChatMessage(String name, String message) {
		this.name = name;
		this.message = message;
	}

	@Override
	public String toString() {
		return name + ": " + message;
	}
}
