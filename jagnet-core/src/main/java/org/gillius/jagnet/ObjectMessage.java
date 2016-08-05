package org.gillius.jagnet;

public abstract class ObjectMessage<T> {
	protected int objectId;
	protected T message;
	private transient Object registeredObject;

	public ObjectMessage() {
	}

	public ObjectMessage(int objectId, T message) {
		this.objectId = objectId;
		this.message = message;
	}

	public int getObjectId() {
		return objectId;
	}

	public void setObjectId(int objectId) {
		this.objectId = objectId;
	}

	public T getMessage() {
		return message;
	}

	public void setMessage(T message) {
		this.message = message;
	}

	public Object getRegisteredObject() {
		return registeredObject;
	}

	public void setRegisteredObject(Object registeredObject) {
		this.registeredObject = registeredObject;
	}
}
