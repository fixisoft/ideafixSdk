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
import java.util.List;
import java.util.Map;

import static com.fixisoft.fix.FixServerFactory.makeServer;
import static com.fixisoft.interfaces.fix.ConnectionType.*;
import static com.fixisoft.interfaces.fix.Protocol.UNIX_DOMAIN_SOCKET;
import static com.fixisoft.interfaces.fix.config.IFixConfig.*;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class OMServerBenchmark {

    public static final String BENCHMARK_CLIENT = "clientBenchmark";
    public static final String BENCHMARK_SERVER = "serverBenchmark";

    public static void main(String[] args) {
        final String name = OMServerBenchmark.class.getSimpleName();
        final boolean useSSL = List.of(args).contains("--ssl");
        final boolean useTCP = List.of(args).contains("--tcp");
        final IFixServer fixServer = makeServer(makeSimpleServerConfig(name,useSSL, useTCP), new OMBenchmarkServerHandler());
        fixServer.run();
    }

    public static IFixConfig makeSimpleServerConfig(final String name, final boolean useSSL, final  boolean useTCP) {
        Map<String,Object> configMap = new HashMap<>(ofEntries(
                entry(BEGIN_STRING, "FIX.4.4"),
                entry(CONNECTION_TYPE, ACCEPTOR),
                entry(DATA_DICTIONARY, "SIMPLE_OM.xml"),
                entry(START_TIME, "00:00:00"),
                entry(END_TIME, "23:59:59"),
                entry(TIME_ZONE,"Europe/Paris"),
                entry(FILE_STORE_PATH, "./ideafix_data/ideafix_server"),
                entry(HEART_BT_INT, 10),
                entry(INCOMING_POOL_SIZES, Map.of(
                        "D", 64,
                        "0", 32,
                        "1", 32,
                        "2", 32,
                        "3", 32,
                        "4", 32,
                        "5", 32,
                        "A", 32
                )),
                entry(OUTGOING_POOL_SIZES, Map.of(
                        "8", 128,
                        "0", 32,
                        "1", 32,
                        "2", 32,
                        "3", 32,
                        "4", 32,
                        "5", 32,
                        "A", 32
                )),
                entry(PERSIST_INCOMING_MESSAGES, false),
                entry(PERSIST_OUTGOING_MESSAGES, false),
                entry(BOSS_EVENT_LOOP_BUSY_WAIT, false),
                entry(WORKER_EVENT_LOOP_BUSY_WAIT, true),
                entry(SO_BUSY_POLL, 50), // depends on System setup needs root
                entry(SOCKET_HOST, "localhost"),
                entry(SOCKET_ACCEPT_PORT, 8080),
                entry(SENDER_COMP_ID, BENCHMARK_SERVER),
                entry(TARGET_COMP_ID, BENCHMARK_CLIENT)
                ));
        if(useSSL) {
            configMap.put(IFixConfig.SOCKET_USE_SSL, true);
            configMap.put(IFixConfig.NEED_CLIENT_AUTH, true);
            configMap.put(IFixConfig.SOCKET_KEY_STORE, "server.jks");
            configMap.put(IFixConfig.SOCKET_KEY_STORE_PASSWORD, "server_password");
            configMap.put(IFixConfig.CIPHER_SUITES,"TLS_AES_128_GCM_SHA256");
        }
        if(useTCP) {
            configMap.put(IFixConfig.SOCKET_ACCEPT_PROTOCOL, Protocol.TCP.name());
        } else {
            configMap.put(SOCKET_ACCEPT_PROTOCOL, UNIX_DOMAIN_SOCKET.name());
            configMap.put(UNIX_DOMAIN_SOCKET_PATH, "/dev/shm/om_benchmark.sock");
        }
        Map<String, Map<String,Object>> sessionProperties = new HashMap<>();
        sessionProperties.put(name, configMap);
        return FixServerFactory.loadConfig(sessionProperties);
    }
}
