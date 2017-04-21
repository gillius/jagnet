package org.gillius.jagnet;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class ObjectManager {
	private static final Consumer<Object> NOOP = x -> {};
	private final Map<Integer, Registration> idToRegistration = new HashMap<>();
	private final Map<Object, Registration> objectToRegistration = new IdentityHashMap<>();

	private int nextId = 0;

	private Connection updateConnection = null;

	private Consumer<? super ObjectCreateMessage> onCreateListener = NOOP;
	private Consumer<? super ObjectUpdateMessage> onUpdateListener = NOOP;
	private Function<Object, Object> sendUpdateTransformation = Function.identity();

	/**
	 * Registers an object with an automatic ID. The existing ID algorithm works only with the assumption that only one
	 * side creates all objects, or that both sides create the same objects in the same order.
	 */
	public Registration registerObject(Object object) {
		return registerObject(nextId, object);
	}

	public Registration registerObject(int id, Object object) {
		nextId = Math.max(id + 1, nextId);

		//Remove existing mapping for object or ID, if any
		Registration old = objectToRegistration.remove(object);
		if (old != null)
			idToRegistration.remove(old.getId());
		old = idToRegistration.remove(id);
		if (old != null)
			objectToRegistration.remove(old.getObject());

		Registration ret = new Registration(id, object);
		idToRegistration.put(id, ret);
		objectToRegistration.put(object, ret);
		return ret;
	}

	public Object getObject(int id) {
		Registration reg = idToRegistration.get(id);
		return reg != null ? reg.getObject() : null;
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
		registerObject(message.getObjectId(), message.getMessage()).setOwned(false);
		onCreateListener.accept(message);
	}

	public void handleUpdate(ObjectUpdateMessage<?> message) {
		Object registeredObject = getObject(message.getObjectId());
		if (registeredObject != null) {
			message.setRegisteredObject(registeredObject);
			onUpdateListener.accept(message);
		}
	}

	public void sendReliableUpdate(Object object) {
		Registration reg = objectToRegistration.get(object);
		if (reg != null && reg.isOwned()) {
			Object toSend = sendUpdateTransformation.apply(reg.getObject());
			updateConnection.sendReliable(new ObjectUpdateMessage<>(reg.getId(), toSend));
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

	public Function<Object, Object> getSendUpdateTransformation() {
		return sendUpdateTransformation;
	}

	/**
	 * Sets the function that generates an update message object given a registered object. The default function is identity,
	 * which sends the entire object's state. The behavior of this function should match with
	 * {@link #setOnUpdateListener(Consumer)}, which receives the update message objects.
	 */
	public void setSendUpdateTransformation(Function<Object, Object> sendUpdateTransformation) {
		this.sendUpdateTransformation = sendUpdateTransformation;
	}

	public Connection getUpdateConnection() {
		return updateConnection;
	}

	/**
	 * Sets the destination for object update messages, which is required to use {@link #sendReliableUpdate(Object)}.
	 */
	public void setUpdateConnection(Connection updateConnection) {
		this.updateConnection = updateConnection;
	}

	public static class Registration {
		private final int id;
		private final Object object;

		private boolean owned = true;

		public Registration(int id, Object object) {
			this.id = id;
			this.object = object;
		}

		public int getId() {
			return id;
		}

		public Object getObject() {
			return object;
		}

		/**
		 * Returns true if this system owns the state of the object. The object manager skips sending updates to the object's
		 * state if it is not owned. By default, the registration assumes the object is owned. Objects received from remote
		 * systems (via {@link ObjectCreateMessage} are assumed owned false.
		 */
		public boolean isOwned() {
			return owned;
		}

		/**
		 * @see #isOwned()
		 */
		public void setOwned(boolean owned) {
			this.owned = owned;
		}
	}
}
