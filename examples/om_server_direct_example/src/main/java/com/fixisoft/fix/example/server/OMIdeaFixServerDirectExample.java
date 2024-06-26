/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.fixisoft.fix.example.server;


import com.fixisoft.fix.FixServerFactory;
import com.fixisoft.interfaces.fix.config.IFixConfig;
import com.fixisoft.interfaces.fix.ConnectionType;
import com.fixisoft.interfaces.fix.IFixServer;
import com.fixisoft.interfaces.fix.Protocol;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.fixisoft.fix.FixServerFactory.makeServer;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class OMIdeaFixServerDirectExample {

    public static final String TEST_CLIENT_1 = "testClient_1";
    public static final String TEST_SERVER_1 = "testServer_1";


    public static final String TEST_CLIENT_2 = "testClient_2";
    public static final String TEST_SERVER_2 = "testServer_2";

    public static void main(String[] args) throws IOException {
        final String name1 = OMIdeaFixServerDirectExample.class.getSimpleName()+"_1";
        final String name2 = OMIdeaFixServerDirectExample.class.getSimpleName()+"_2";
        final IFixServer fixServer = makeServer(makeSimpleServerConfig(name1, name2), new OMServerIncomingDirectHandler());
        fixServer.run();
    }

    public static IFixConfig makeSimpleServerConfig(final String name1, final String name2) {
       Map<String,Object> session1 = ofEntries(
                entry(IFixConfig.BEGIN_STRING, "FIX.4.4"),
                entry(IFixConfig.CONNECTION_TYPE, ConnectionType.ACCEPTOR),
                entry(IFixConfig.DATA_DICTIONARY, "SIMPLE_OM.xml"),
               entry(IFixConfig.START_TIME, "00:00:00"),
               entry(IFixConfig.END_TIME, "23:59:59"),
                entry(IFixConfig.TIME_ZONE,"Europe/Paris"),
               /* entry(START_DAY,"Monday"),
                entry(END_DAY,"Friday"),*/
                entry(IFixConfig.FILE_STORE_PATH, "./messages/" + name1),
                entry(IFixConfig.HEART_BT_INT, 10),
                entry(IFixConfig.INCOMING_POOL_SIZES, Map.of(
                        "D", 512,
                        "0", 64,
                        "1", 64,
                        "2", 64,
                        "3", 64,
                        "4", 64,
                        "5", 64,
                        "A", 64
                )),
                entry(IFixConfig.OUTGOING_POOL_SIZES, Map.of(
                        "8", 1024,
                        "0", 64,
                        "1", 64,
                        "2", 64,
                        "3", 64,
                        "4", 64,
                        "5", 64,
                        "A", 64
                )),
                entry(IFixConfig.SENDER_COMP_ID, TEST_SERVER_1),
                entry(IFixConfig.TARGET_COMP_ID, TEST_CLIENT_1),
                entry(IFixConfig.SOCKET_ACCEPT_PROTOCOL, Protocol.TCP.name()),
                entry(IFixConfig.UNIX_DOMAIN_SOCKET_PATH, "/tmp/fix.sock"),
                entry(IFixConfig.SOCKET_HOST, "localhost"),
                entry(IFixConfig.SOCKET_ACCEPT_PORT, 8080));

/*        Map<String,Object> session2 = ofEntries(
                entry(BEGIN_STRING, "FIX.4.4"),
                entry(CONNECTION_TYPE, ACCEPTOR),
                entry(DATA_DICTIONARY, "SIMPLE_OM.xml"),
                entry(START_TIME, "00:00:00"),
                entry(END_TIME, "23:59:00"),
                entry(TIME_ZONE,"Europe/Paris"),
               *//* entry(START_DAY,"Monday"),
                entry(END_DAY,"Friday"),*//*
                entry(FILE_STORE_PATH, "./test_2/" + name2),
                entry(HEART_BT_INT, 10),
                entry(INCOMING_POOL_SIZES, Map.of(
                        "D", 8192,
                        "0", 128,
                        "1", 128,
                        "2", 128,
                        "3", 128,
                        "4", 128,
                        "5", 128,
                        "A", 128
                )),
                entry(OUTGOING_POOL_SIZES, Map.of(
                        "8", 16384,
                        "0", 128,
                        "1", 128,
                        "2", 128,
                        "3", 128,
                        "4", 128,
                        "5", 128,
                        "A", 128
                )),
                entry(SENDER_COMP_ID, TEST_SERVER_2),
                entry(TARGET_COMP_ID, TEST_CLIENT_2),
                entry(SOCKET_ACCEPT_PROTOCOL, TCP.name()),
          //      entry(UNIX_DOMAIN_SOCKET_PATH, "/tmp/fix.sock"),
                entry(SOCKET_HOST, "localhost"),
                entry(SOCKET_ACCEPT_PORT, 8080));*/

        Map<String, Map<String,Object>> sessionProperties = new HashMap<>();
        sessionProperties.put(name1, session1);
    //    sessionProperties.put(name2, session2);

        return FixServerFactory.loadConfig(sessionProperties);
    }
}
