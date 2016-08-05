package org.gillius.jagnet;

import com.esotericsoftware.kryo.Kryo;

import java.util.List;

import static java.util.Arrays.asList;

public class FrameworkMessages {
	public static List<Class<?>> getMessageTypes() {
		return asList(
				ChatMessage.class,
				TimeSyncRequest.class,
				TimeSyncResponse.class,
				ObjectCreateMessage.class,
				ObjectUpdateMessage.class
		             );
	}

	public static void register(Kryo kryo) {
		for (Class<?> clazz : getMessageTypes()) {
			kryo.register(clazz);
		}
	}
}
