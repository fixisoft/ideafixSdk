/*
 * Copyright (c) Pierre-Yves Peton 2024.
 * All rights reserved
 */
package com.fixisoft.fix.example.server;


import com.fixisoft.fix.FixServerFactory;
import com.fixisoft.interfaces.fix.config.IFixConfig;
import com.fixisoft.interfaces.fix.IFixServer;

import java.util.HashMap;
import java.util.Map;

import static com.fixisoft.fix.FixServerFactory.makeServer;
import static com.fixisoft.interfaces.fix.ConnectionType.*;
import static com.fixisoft.interfaces.fix.Protocol.UNIX_DOMAIN_SOCKET;
import static com.fixisoft.interfaces.fix.config.IFixConfig.*;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class OMDirectServerBenchmark {

    public static final String BENCHMARK_CLIENT = "clientBenchmark";
    public static final String BENCHMARK_SERVER = "serverBenchmark";

    public static void main(String[] args) {
        final String name = OMDirectServerBenchmark.class.getSimpleName();
        final IFixServer fixServer = makeServer(makeSimpleServerConfig(name), new OMBenchmarkDirectServerHandler());
        fixServer.run();
    }

    public static IFixConfig makeSimpleServerConfig(final String name) {
        Map<String,Object> session = ofEntries(
                entry(BEGIN_STRING, "FIX.4.4"),
                entry(CONNECTION_TYPE, ACCEPTOR),
                entry(DATA_DICTIONARY, "DIRECT_SIMPLE_OM.xml"),
                entry(START_TIME, "00:00:00"),
                entry(END_TIME, "23:59:59"),
                entry(TIME_ZONE,"Europe/Paris"),
                entry(FILE_STORE_PATH, "./ideafix_data/ideafix_server"),
                entry(HEART_BT_INT, 10),
                entry(INCOMING_POOL_SIZES, Map.of(
                        "D", 128,
                        "0", 64,
                        "1", 64,
                        "2", 64,
                        "3", 64,
                        "4", 64,
                        "5", 64,
                        "A", 64
                )),
                entry(OUTGOING_POOL_SIZES, Map.of(
                        "8", 256,
                        "0", 64,
                        "1", 64,
                        "2", 64,
                        "3", 64,
                        "4", 64,
                        "5", 64,
                        "A", 64
                )),
                entry(PERSIST_INCOMING_MESSAGES, false),
                entry(BOSS_EVENT_LOOP_BUSY_WAIT, false),
                entry(WORKER_EVENT_LOOP_BUSY_WAIT, true),
                entry(SO_BUSY_POLL, 50), // depends on System setup needs root
                entry(SOCKET_ACCEPT_PROTOCOL, UNIX_DOMAIN_SOCKET.name()),
                entry(UNIX_DOMAIN_SOCKET_PATH, "/dev/shm/om_benchmark.sock"),
            //    entry(IFixConfig.SOCKET_ACCEPT_PROTOCOL, Protocol.TCP.name()),
                entry(SOCKET_HOST, "localhost"),
                entry(SOCKET_ACCEPT_PORT, 8080),
                entry(SENDER_COMP_ID, BENCHMARK_SERVER),
                entry(TARGET_COMP_ID, BENCHMARK_CLIENT)
                );
        Map<String, Map<String,Object>> sessionProperties = new HashMap<>();
        sessionProperties.put(name, session);
        return FixServerFactory.loadConfig(sessionProperties);
    }
}
