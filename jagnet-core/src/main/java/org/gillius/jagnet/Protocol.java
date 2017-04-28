package org.gillius.jagnet;

public enum Protocol {
	/**
	 * Communication exclusively over TCP.
	 */
	TCP,

	/**
	 * Communication over Websockets
	 */
	WS,

	//TODO: future protocols TCPUDP, WSS
}
