/*
 * Copyright (c) Pierre-Yves Peton 2024.
 * All rights reserved
 */
package com.fixisoft.fix.example.server;


import com.fixisoft.fix.FixServerFactory;
import com.fixisoft.interfaces.fix.config.IFixConfig;
import com.fixisoft.interfaces.fix.ConnectionType;
import com.fixisoft.interfaces.fix.IFixServer;
import com.fixisoft.interfaces.fix.Protocol;

import java.util.HashMap;
import java.util.Map;

import static com.fixisoft.fix.FixServerFactory.makeServer;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class OMServerExample {

    public static final String TEST_CLIENT_1 = "testClient_1";
    public static final String TEST_SERVER_1 = "testServer_1";

    public static void main(String[] args)  {
        final String name = OMServerExample.class.getSimpleName()+"_1";
        final IFixServer fixServer = makeServer(makeSimpleServerConfig(name), new TestServerIncomingHandler());
        fixServer.run();
    }

    public static IFixConfig makeSimpleServerConfig(final String name) {
       Map<String,Object> session1 = ofEntries(
                entry(IFixConfig.BEGIN_STRING, "FIX.4.4"),
                entry(IFixConfig.CONNECTION_TYPE, ConnectionType.ACCEPTOR),
                entry(IFixConfig.DATA_DICTIONARY, "SIMPLE_OM.xml"),
               entry(IFixConfig.START_TIME, "00:00:00"),
               entry(IFixConfig.END_TIME, "23:59:59"),
                entry(IFixConfig.TIME_ZONE,"Europe/Paris"),
                entry(IFixConfig.FILE_STORE_PATH, "./messages/" + name),
                entry(IFixConfig.HEART_BT_INT, 10),
                entry(IFixConfig.INCOMING_POOL_SIZES, Map.of(
                        "D", 1024,
                        "0", 128,
                        "1", 128,
                        "2", 128,
                        "3", 128,
                        "4", 128,
                        "5", 128,
                        "A", 128
                )),
                entry(IFixConfig.OUTGOING_POOL_SIZES, Map.of(
                        "8", 2048,
                        "0", 128,
                        "1", 128,
                        "2", 128,
                        "3", 128,
                        "4", 128,
                        "5", 128,
                        "A", 128
                )),
                entry(IFixConfig.SENDER_COMP_ID, TEST_SERVER_1),
                entry(IFixConfig.TARGET_COMP_ID, TEST_CLIENT_1),
               entry(IFixConfig.SOCKET_ACCEPT_PROTOCOL, Protocol.TCP.name()),
                entry(IFixConfig.SOCKET_HOST, "localhost"),
                entry(IFixConfig.SOCKET_ACCEPT_PORT, 8080));

        Map<String, Map<String,Object>> sessionProperties = new HashMap<>();
        sessionProperties.put(name, session1);
        return FixServerFactory.loadConfig(sessionProperties);
    }
}
