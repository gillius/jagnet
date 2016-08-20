package org.gillius.jagnet;

public class SingleConnectionListenerContext implements ConnectionListenerContext {
	private final Connection connection;

	public SingleConnectionListenerContext(Connection connection) {
		this.connection = connection;
	}

	@Override
	public Connection getConnection() {
		return connection;
	}

	@Override
	public void consumeCurrentEvent() {
		//do nothing as this is not a listener chain
	}
}
