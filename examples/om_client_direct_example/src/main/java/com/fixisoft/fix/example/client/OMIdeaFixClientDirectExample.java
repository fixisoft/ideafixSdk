/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.fixisoft.fix.example.client;


import com.fixisoft.interfaces.fix.ConnectionType;
import com.fixisoft.fix.FixClientFactory;
import com.fixisoft.interfaces.fix.IFixClient;
import com.fixisoft.interfaces.fix.Protocol;
import com.fixisoft.interfaces.fix.config.IFixConfig;

import java.util.Map;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class OMIdeaFixClientDirectExample {

    public static final String TEST_CLIENT_1 = "testClient_1";
    public static final String TEST_SERVER_1 = "testServer_1";


    public static void main(String[] args)  {
        final String name = OMIdeaFixClientDirectExample.class.getSimpleName();
        final IFixClient fixClient = FixClientFactory.makeClient(makeSimpleClientConfig(name), name, new OMClientIncomingDirectHandler());
        fixClient.run();
    }

    public static IFixConfig makeSimpleClientConfig(final String name) {
        return FixClientFactory.loadConfig(name, ofEntries(
                entry(IFixConfig.BEGIN_STRING, "FIX.4.4"),
                entry(IFixConfig.CONNECTION_TYPE, ConnectionType.INITIATOR),
                entry(IFixConfig.DATA_DICTIONARY, "SIMPLE_OM.xml"),
                entry(IFixConfig.START_TIME, "00:00:00"),
                entry(IFixConfig.END_TIME, "23:59:59"),
                entry(IFixConfig.TIME_ZONE, "Europe/Paris"),
           /*     entry(START_DAY, "Monday"),
                entry(END_DAY, "Friday"),*/
                entry(IFixConfig.FILE_STORE_PATH, "./messages/" + name),
                entry(IFixConfig.HEART_BT_INT, 10),
                entry(IFixConfig.INCOMING_POOL_SIZES, Map.of(
                        "8", 1024,
                        "0", 64,
                        "1", 64,
                        "2", 64,
                        "3", 64,
                        "4", 64,
                        "5", 64,
                        "A", 64
                )),
                entry(IFixConfig.OUTGOING_POOL_SIZES, Map.of(
                        "D", 512,
                        "0", 64,
                        "1", 64,
                        "2", 64,
                        "3", 64,
                        "4", 64,
                        "5", 64,
                        "A", 64
                )),
                entry(IFixConfig.PERSIST_INCOMING_MESSAGES, false),
                entry(IFixConfig.SENDER_COMP_ID, TEST_CLIENT_1),
                entry(IFixConfig.TARGET_COMP_ID, TEST_SERVER_1),
                entry(IFixConfig.SOCKET_CONNECT_PROTOCOL, Protocol.TCP.name()),
                entry(IFixConfig.UNIX_DOMAIN_SOCKET_PATH, "/tmp/fix.sock"),
                entry(IFixConfig.SOCKET_HOST, "localhost"),
                entry(IFixConfig.SOCKET_PORT, 8080)));
    }
}
