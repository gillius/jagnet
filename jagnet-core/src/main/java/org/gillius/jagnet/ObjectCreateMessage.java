package org.gillius.jagnet;

public class ObjectCreateMessage<T> extends ObjectMessage<T> {
	public ObjectCreateMessage() {
	}

	public ObjectCreateMessage(int objectId, T object) {
		super(objectId, object);
	}

	@Override
	public String toString() {
		return "Create message " + getObjectId() + ": " + getMessage();
	}
}
