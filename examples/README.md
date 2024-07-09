IdeaFIX SDK
===========

Pre-requisites 

* JDK 17
* Gradle 8.0+
* Maven 2.6+

IdeaFIX requires a JDK 17 minimum. Examples are written in JAVA, 
but it's possible to your JVM language of choice.
Installer is here : https://jdk.java.net/

To run the install script, you’ll need Maven 2.6+. 
Installer is here : https://maven.apache.org/install.html

To run the example you'll need gradle 8.0+.
Installer is here : https://gradle.org/install/

This documentation will grow overtime but to start wih, there are 5 examples.

First 4 are :

* ```om_client_example```
* ```om_server_example```
* ```md_client_example```
* ```md_server_example```

They defined a common SIMPLE_OM.xml dictionary

Note the xml format follows Quickfix convention, so Quickfix dictionary
(FIX44.xml, FIX42.xml, etc.) can be re-used.

FixClientExample and FixServerExample are the main classes
They both define a basic configuration. Those familiar with QuickFIX will be at ease,
the same configuration conventions are used.

No ```IFixMessageInitializer``` interface is used, minimum header and trailer configuration is used
However, some counterparties may expect additional fields.

Implementing ```IFixMessageInitializer``` will help dealing with this case.

The business logic is essentially in 2 classes :

* ```TestClientIncomingHandler```
* ```TestServerIncomingHandler```

To test the apps, you simply need to run the following commands in the examples directory.

```groovy  
gradle om_client_example:run
gradle om_server_example:run
```

or 

```groovy  
gradle md_client_example:run
gradle md_server_example:run
```

and look at the traffic

```bash
tail -f om_client_example/fixclient.log
tail -f om_server_example/fixserver.log
```

```bash
tail -f md_client_example/MDfixclient.log
tail -f md_server_example/MDfixserver.log
```

And lastly, ```ctrader_md_client_example``` is an example to connect to CTrader FIX API https://help.ctrader.com/fix/
It's possibible to freely create a demo account

