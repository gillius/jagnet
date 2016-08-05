package org.gillius.jagnet;

import com.esotericsoftware.kryo.Kryo;
import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;

import static java.util.Arrays.asList;

public class KryoCopier {
	private final Kryo kryo = new Kryo();
	private final FixedObjectInstantiator instantiator = new FixedObjectInstantiator();

	public KryoCopier() {
		kryo.setInstantiatorStrategy(new FixedInstantiatorStrategy(instantiator));
	}

	public Kryo getKryo() {
		return kryo;
	}

	public KryoCopier register(Class<?>... c) {
		return register(asList(c));
	}

	public KryoCopier register(Iterable<Class<?>> classes) {
		for (Class<?> clazz : classes) {
			kryo.register(clazz);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> void copy(T from, T to) {
		if (from == null || to == null)
			throw new NullPointerException();
		//Kryo can use "unsafe" offset-based copying which will actually crash the VM if the types are not exactly the same.
		if (from.getClass() != to.getClass()) {
			throw new IllegalArgumentException("from (" + from.getClass() + ") and to (" + to.getClass() +
			                                   ") must be of the same class");
		}
		instantiator.setObject(to);
		kryo.copyShallow(from);
	}

	public static class FixedInstantiatorStrategy implements InstantiatorStrategy {
		private final ObjectInstantiator instantiator;

		public FixedInstantiatorStrategy(ObjectInstantiator instantiator) {
			this.instantiator = instantiator;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> type) {
			return instantiator;
		}
	}

	public static class FixedObjectInstantiator<T> implements ObjectInstantiator<T> {
		private T object;

		@Override
		public T newInstance() {
			if (object == null)
				throw new AssertionError("newInstance called multiple times");
			T ret = object;
			object = null;
			return ret;
		}

		public T getObject() {
			return object;
		}

		public void setObject(T object) {
			this.object = object;
		}
	}
}
