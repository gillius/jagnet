package org.gillius.jagnet;

public class ObjectUpdateMessage<T> extends ObjectMessage<T> {
	public ObjectUpdateMessage() {
	}

	public ObjectUpdateMessage(int objectId, T object) {
		super(objectId, object);
	}

	@Override
	public String toString() {
		return "Update message " + getObjectId() + ": " + getMessage() + " for " + getRegisteredObject();
	}
}
