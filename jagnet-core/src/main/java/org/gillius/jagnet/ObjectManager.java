package org.gillius.jagnet;

import java.util.HashMap;
import java.util.function.Consumer;

public class ObjectManager {
	private static final Consumer<Object> NOOP = x -> {};
	private final HashMap<Integer, Object> objects = new HashMap<>();

	private Consumer<? super ObjectCreateMessage> onCreateListener = NOOP;
	private Consumer<? super ObjectUpdateMessage> onUpdateListener = NOOP;

	public void registerObject(int id, Object object) {
		objects.put(id, object);
	}

	public Object getObject(int id) {
		return objects.get(id);
	}

	public boolean handleMessage(Object message) {
		if (message instanceof ObjectMessage) {
			if (message instanceof ObjectCreateMessage) {
				handleCreate((ObjectCreateMessage) message);
			} else if (message instanceof ObjectUpdateMessage) {
				handleUpdate((ObjectUpdateMessage) message);
			}
			return true;
		} else {
			return false;
		}
	}

	public void handleCreate(ObjectCreateMessage<?> message) {
		objects.put(message.getObjectId(), message.getMessage());
		onCreateListener.accept(message);
	}

	public void handleUpdate(ObjectUpdateMessage<?> message) {
		Object registeredObject = getObject(message.getObjectId());
		if (registeredObject != null) {
			message.setRegisteredObject(registeredObject);
			onUpdateListener.accept(message);
		}
	}

	public Consumer<? super ObjectCreateMessage> getOnCreateListener() {
		return onCreateListener;
	}

	public void setOnCreateListener(Consumer<? super ObjectCreateMessage> onCreateListener) {
		this.onCreateListener = onCreateListener;
	}

	public Consumer<? super ObjectUpdateMessage> getOnUpdateListener() {
		return onUpdateListener;
	}

	public void setOnUpdateListener(Consumer<? super ObjectUpdateMessage> onUpdateListener) {
		this.onUpdateListener = onUpdateListener;
	}
}
