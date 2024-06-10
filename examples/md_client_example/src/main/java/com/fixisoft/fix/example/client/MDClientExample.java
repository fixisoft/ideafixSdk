/*
 * Copyright (c) Pierre-Yves Peton 2024.
 * All rights reserved
 */
package com.fixisoft.fix.example.client;


import com.fixisoft.interfaces.fix.ConnectionType;
import com.fixisoft.fix.FixClientFactory;
import com.fixisoft.interfaces.fix.IFixClient;
import com.fixisoft.interfaces.fix.Protocol;
import com.fixisoft.interfaces.fix.config.IFixConfig;
import com.fixisoft.interfaces.fix.session.NoopMessageInitializer;

import java.util.Map;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class MDClientExample {

    public static final String TEST_CLIENT_1 = "testClient_1";
    public static final String TEST_SERVER_1 = "testServer_1";


    public static void main(String[] args)  {
        final String name = MDClientExample.class.getSimpleName();
        final IFixClient fixClient = FixClientFactory.makeClient(makeSimpleClientConfig(name), name,new TestMDClientIncomingHandler(), new NoopMessageInitializer());
        fixClient.run();
    }

    public static IFixConfig makeSimpleClientConfig(final String name) {
        return FixClientFactory.loadConfig(name, ofEntries(
                entry(IFixConfig.BEGIN_STRING, "FIX.4.4"),
                entry(IFixConfig.CONNECTION_TYPE, ConnectionType.INITIATOR),
                entry(IFixConfig.DATA_DICTIONARY, "SIMPLE_MD.xml"),
                entry(IFixConfig.START_TIME, "00:00:00"),
                entry(IFixConfig.END_TIME, "23:59:59"),
                entry(IFixConfig.TIME_ZONE, "Europe/Paris"),
                entry(IFixConfig.FILE_STORE_PATH, "./messages/" + name),
                entry(IFixConfig.HEART_BT_INT, 10),
                entry(IFixConfig.INCOMING_POOL_SIZES, Map.of(
                        "W", 2048,
                        "0", 128,
                        "1", 128,
                        "2", 128,
                        "3", 128,
                        "4", 128,
                        "5", 128,
                        "A", 128
                )),
                entry(IFixConfig.OUTGOING_POOL_SIZES, Map.of(
                        "V", 1024,
                        "0", 128,
                        "1", 128,
                        "2", 128,
                        "3", 128,
                        "4", 128,
                        "5", 128,
                        "A", 128
                )),
                entry(IFixConfig.SENDER_COMP_ID, TEST_CLIENT_1),
                entry(IFixConfig.TARGET_COMP_ID, TEST_SERVER_1),
                entry(IFixConfig.SOCKET_CONNECT_PROTOCOL, Protocol.TCP.name()),
                entry(IFixConfig.SOCKET_HOST, "localhost"),
                entry(IFixConfig.SOCKET_PORT, 8080)));
    }
}
