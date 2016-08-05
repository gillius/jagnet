package org.gillius.jagnet.kryonet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import static java.util.Arrays.asList;

abstract class KryonetClientServerBase {
	protected int port = -1;

	protected abstract EndPoint getEndPoint();

	public void setPort(int port) {
		this.port = port;
	}

	public void registerMessages(Iterable<Class<?>> messageTypes) {
		Kryo kryo = getEndPoint().getKryo();
		for (Class<?> type : messageTypes) {
			kryo.register(type);
		}
	}

	public void registerMessages(Class<?>... messageTypes) {
		registerMessages(asList(messageTypes));
	}

	public void close() {
		getEndPoint().close();
	}
}
