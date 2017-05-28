Jagnet - Java Game Networking
=============================

Jagnet is a game networking library for Java applications with the ultimate aim to provide easy and rapid integration of
network functionality into indie/hobby-level games and proxy/firewall/NAT solutions to make it realistic for users to
actually connect to each other, especially for those with home routers having NAT and standard Windows firewalls.

Current State
-------------

The current state is "functional prototype". Things are working, but error handling and documentation could be improved,
and the API is not completely proven so there may be some missed use cases. As such, the API is not guaranteed to be
stable and compatibility-breaking changes could be introduced.

The following features are implemented:

* Clock synchronization based on an NTP-like algorithm
* Deal with the realities of commonplace NAT and firewalls (via jagnet-proxy-server)
* TCP and HTTP/websocket transports supported
* Ability to define messages simply by declaring POJO beans (plain old Java object)
* Object manager to register/create objects and update them remotely
* Full control over when messages are processed -- for example in a game you can process message between logic loop
  updates so there is no need to worry about threading or race conditions.

Demo
----

There is a working example of a networked ball and paddle game based on the example from
[jalleg](https://github.com/gillius/jalleg). To run the game locally, you can build and run using Gradle. Java 8 and an
Internet connection is all that is needed (Gradle downloads itself):

Run server on localhost port 56238:
```
gradlew runServer
```

Run client to connect to localhost port 56238:
```
gradlew runClient
```

The tasks can take an optional `-Puri=<URI>` parameter to specify a listening configuration and mode.

URI Examples
------------

* tcp://0.0.0.0:56238 - (servers only) Listen on any local address on port 56238 using direct TCP protocol
* tcp://localhost:56238 - Listen for localhost only (servers), or connect to localhost (clients)
* proxy+tcp://localhost:56238?uniquetag - Connect to proxy server to match up with other user also connecting with
  `uniquetag` (clients), or set up a remote listening service at `service:uniquetag` (servers).
* ws://localhost:80/websocket - Listen on localhost (servers) or connect to localhost (clients) using HTTP/websockets
  protocol over port 80, with the HTTP path being /websocket.
* proxy+ws://localhost:80/websocket?uniquetag - Like proxy+tcp example above but using HTTP/websockets protocol

Proxy/Firewall/NAT Support
--------------------------

There is also a proxy server, jagnet-proxy-server, which can be used for connections behind NAT and firewalls that block
inbound traffic. If two clients connect to the proxy server and present the same unique tag, the proxy will route the
connections to each other. The proxy server does not parse or process any data and so can be used for any application.

A proxy service registration mode is also supported for "remote servers". In this mode a "server" connects to the
proxy and registers a unique service tag. Each client that connects to that service tag generates a new token to the
remote server, which then creates a new connection to the proxy, presenting that tag. This allows a service to connect
to multiple clients via the proxy.

The proxy server and clients also support websockets, so that it is possible to send traffic through reverse HTTP
proxies common in cloud or managed hosting environments like Heroku, although there is no clustering capability so you
cannot have multiple host servers, unless you implement it yourself. Included is a Procfile configuration to run the
jagnet-proxy-server in Heroku simply by pointing your app to this github repo.

However, at this time the client does not support connecting from behind an explicit HTTP proxy as would be typically
found in "corporate" or some academic environments, nor is HTTPS supported. However, the current proxy capabilities
allow for connecting through firewalls and NAT, like those commonly found in home Internet connections.

To run a local instance of proxy server:

```
gradlew runProxyServer -Pport=56238 -Pws=false
```

If port is specified, chooses the local port to listen on. If optional parameter ws is specified and is true, then
websocket mode is used.

Technologies
------------

The library is built on the following:

* Java 8+ (closures and functional interfaces)
* [Netty](http://netty.io/): an asynchronous event-driven network application framework
* [Kryo](https://github.com/EsotericSoftware/kryo): serialization library that is very fast and generates small, binary
  messages. This eliminates the need to write custom serialization code -- just define a class and that is your packet.
* [slf4j](http://www.slf4j.org/): logging library facade to interface with your desired logging system (or none at all)

Future Goals
------------

* Game lobby server with create/search/join capability, potentially with REST API and potentially with HTML/JS interface.
* UDP support
* SSL support
* HTTP Proxy support for websockets transport