Jagnet - Java Game Networking
=============================

Jagnet is a game networking library for Java applications with the ultimate aim to provide easy and rapid integration of
network functionality into indie/hobby-level games.

The current status of the library is very experimental, but there is a working example of a networked ball and paddle
game based on the example from [jalleg](https://github.com/gillius/jalleg). To run the game locally, you can build and
run using Gradle. Java 8 and an Internet connection is all that is needed (Gradle downloads itself):

Run server on localhost port 56238:
```
gradlew runServer
```

Run client to connect to localhost port 56238:
```
gradlew runClient
```

There is also a proxy server, jagnet-proxy-server, which can be used for connections behind NAT and firewalls that block
inbound traffic. If two clients connect to the proxy server and present the same unique tag, the proxy will route the
connections to each other. The proxy server does not parse or process any data and so can be used for any application.

There is a start of an object management framework that allows you to register network objects and route messages to
them.

Technologies
------------

The library is built on the following:

* Java 8+ (closures and functional interfaces)
* [Netty](http://netty.io/): an asynchronous event-driven network application framework
* [Kryo](https://github.com/EsotericSoftware/kryo): serialization library that is very fast and generates small, binary
  messages. This eliminates the need to write custom serialization code -- just define a class and that is your packet.
* [slf4j](http://www.slf4j.org/): logging library facade to interface with your desired logging system (or none at all)

Goals
-----

Here are some of my goals:

* Support rapid and easy development of indie/hobby-level networking games
* Game time synchronization based on an NTP-like algorithm
* Deal with the realities of commonplace NAT and firewalls (proxy server)
* Support TCP and UDP connections or simultaneous use of both
* Ability to define messages simply by declaring POJO beans (plain old Java object)
* As close as possible, ability to mark an object as "remote" and have the network code automatically update the fields
  when messages come in. Optionally allow a handler taking an update message and "remote" object and updating the remote
  object.
* Full control over when messages are processed -- for example in a game you can process message between logic loop
  updates so there is no need to worry about threading or race conditions.
  
More future goals:
* Game lobby server with create/search/join capability.
* Leverage the extra capabilities possible with Netty including:
  * SSL support
  * Websocket support -- allow to host/proxy games through firewalls allowing HTTP only and host servers on free or
    inexpensive cloud services that only support incoming HTTP/Websocket connections.