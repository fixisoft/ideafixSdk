/*
 * Copyright (c) 2024 Pierre-Yves Peton  All Rights Reserved
 */
package com.fixisoft.fix.example.client;


import com.fixisoft.interfaces.fix.ConnectionType;
import com.fixisoft.fix.FixClientFactory;
import com.fixisoft.interfaces.fix.IFixClient;
import com.fixisoft.interfaces.fix.Protocol;
import com.fixisoft.interfaces.fix.config.IFixConfig;
import com.fixisoft.interfaces.fix.session.NoopMessageInitializer;

import java.util.Map;

import static com.fixisoft.interfaces.fix.config.IFixConfig.*;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class OMDirectClientBenchmark {

    public static final String BENCHMARK_CLIENT = "clientBenchmark";
    public static final String BENCHMARK_SERVER = "serverBenchmark";


    public static void main(String[] args) {
        final String name = OMDirectClientBenchmark.class.getSimpleName();
        final IFixClient fixClient = FixClientFactory.makeClient(makeSimpleClientConfig(name), name, new OMBenchmarkDirectClientHandler(), new NoopMessageInitializer());
        fixClient.run();
    }

    public static IFixConfig makeSimpleClientConfig(final String name) {
        return FixClientFactory.loadConfig(name, ofEntries(
                entry(BEGIN_STRING, "FIX.4.4"),
                entry(CONNECTION_TYPE, ConnectionType.INITIATOR),
                entry(DATA_DICTIONARY, "DIRECT_SIMPLE_OM.xml"),
                entry(START_TIME, "00:00:00"),
                entry(END_TIME, "23:59:59"),
                entry(TIME_ZONE, "Europe/Paris"),
                entry(FILE_STORE_PATH, "./ideafix_data/ideafix_client"),
                entry(HEART_BT_INT, 10),
                entry(INCOMING_POOL_SIZES, Map.of(
                        "8", 256,
                        "0", 64,
                        "1", 64,
                        "2", 64,
                        "3", 64,
                        "4", 64,
                        "5", 64,
                        "A", 64
                )),
                entry(OUTGOING_POOL_SIZES, Map.of(
                        "D", 128,
                        "0", 64,
                        "1", 64,
                        "2", 64,
                        "3", 64,
                        "4", 64,
                        "5", 64,
                        "A", 64
                )),
                entry(PERSIST_INCOMING_MESSAGES, false),
                entry(WORKER_EVENT_LOOP_BUSY_WAIT, true),
                entry(SO_BUSY_POLL, 50), // depends on System setup needs root
                entry(SOCKET_CONNECT_PROTOCOL, Protocol.UNIX_DOMAIN_SOCKET.name()),
                entry(UNIX_DOMAIN_SOCKET_PATH, "/dev/shm/om_benchmark.sock"),
             //   entry(IFixConfig.SOCKET_CONNECT_PROTOCOL, Protocol.TCP.name()),
                entry(SOCKET_HOST, "localhost"),
                entry(SOCKET_PORT, 8080),
                entry(SENDER_COMP_ID, BENCHMARK_CLIENT),
                entry(TARGET_COMP_ID, BENCHMARK_SERVER)
        ));
    }
}
