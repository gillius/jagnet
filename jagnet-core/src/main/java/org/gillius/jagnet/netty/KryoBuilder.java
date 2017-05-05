package org.gillius.jagnet.netty;

import com.esotericsoftware.kryo.Kryo;
import org.gillius.jagnet.FrameworkMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class KryoBuilder implements Supplier<Kryo> {
	private final List<Class<?>> messageTypes = new ArrayList<>();

	public void registerMessages(Iterable<Class<?>> messageTypes) {
		for (Class<?> messageType : messageTypes) {
			this.messageTypes.add(messageType);
		}
	}

	public void registerMessages(Class<?>... messageTypes) {
		Collections.addAll(this.messageTypes, messageTypes);
	}

	@Override
	public Kryo get() {
		return build(messageTypes);
	}

	public static Kryo build(Iterable<Class<?>> messageTypes) {
		Kryo ret = new Kryo();
		ret.setReferences(false);
		ret.setRegistrationRequired(true);
		FrameworkMessages.register(ret);
		messageTypes.forEach(ret::register);
		return ret;
	}
}
