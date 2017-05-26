package org.gillius.jagnet.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleUtil {
	private static final Logger log = LoggerFactory.getLogger(ExampleUtil.class);

	public static String getUri(String args[]) {
		if (args.length > 1) {
			return args[0];
		} else {
			String uri = "tcp://localhost:54555";
			//alternative uri = "proxy+ws://localhost:56238/websocket?ServerExample"
			log.info("Using default URI parameter {}", uri);
			return uri;
		}
	}
}
