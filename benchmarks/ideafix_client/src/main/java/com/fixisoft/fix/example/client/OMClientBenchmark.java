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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fixisoft.interfaces.fix.config.IFixConfig.*;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class OMClientBenchmark {

    public static final String BENCHMARK_CLIENT = "client";
    public static final String BENCHMARK_SERVER = "server";


    public static void main(String[] args) {
        final String name = OMClientBenchmark.class.getSimpleName();
        final boolean useSSL = List.of(args).contains("--ssl");
        final boolean useTCP = List.of(args).contains("--tcp");
        final IFixClient fixClient = FixClientFactory.makeClient(makeSimpleClientConfig(name,useSSL,useTCP), name, new OMBenchmarkClientHandler(), new NoopMessageInitializer());
        fixClient.run();
    }

    public static IFixConfig makeSimpleClientConfig(final String name, final  boolean useSSL, boolean useTCP) {
        HashMap<String,Object> configMap = new HashMap<>(ofEntries(
                entry(BEGIN_STRING, "FIX.4.4"),
                entry(CONNECTION_TYPE, ConnectionType.INITIATOR),
                entry(DATA_DICTIONARY, "SIMPLE_OM.xml"),
                entry(START_TIME, "00:00:00"),
                entry(END_TIME, "23:59:59"),
                entry(TIME_ZONE, "Europe/Paris"),
                entry(FILE_STORE_PATH, "./ideafix_data/ideafix_client"),
                entry(HEART_BT_INT, 10),
                entry(INCOMING_POOL_SIZES, Map.of(
                        "8", 256,
                        "0", 32,
                        "1", 32,
                        "2", 32,
                        "3", 32,
                        "4", 32,
                        "5", 32,
                        "A", 32
                )),
                entry(OUTGOING_POOL_SIZES, Map.of(
                        "D", 128,
                        "0", 32,
                        "1", 32,
                        "2", 32,
                        "3", 32,
                        "4", 32,
                        "5", 32,
                        "A", 32
                )),
                entry(PERSIST_INCOMING_MESSAGES, false),
                entry(WORKER_EVENT_LOOP_BUSY_WAIT, true),
                entry(SO_BUSY_POLL, 50), // depends on System setup needs root
                entry(SO_SNDBUF, 256),
                entry(SO_RCVBUF,512),
                entry(TCP_NOT_SENT_LOWAT, 1024),
                entry(SOCKET_HOST, "localhost"),
                entry(SOCKET_PORT, 8080),
                entry(SENDER_COMP_ID, BENCHMARK_CLIENT),
                entry(TARGET_COMP_ID, BENCHMARK_SERVER)));
        if(useSSL) {
            configMap.put(IFixConfig.SOCKET_USE_SSL, true);
            configMap.put(IFixConfig.SOCKET_KEY_STORE, "client.jks");
            configMap.put(IFixConfig.SOCKET_KEY_STORE_PASSWORD, "client_password");
            configMap.put(IFixConfig.CIPHER_SUITES,"TLS_AES_128_GCM_SHA256");
        }
        if(useTCP) {
            configMap.put(SOCKET_CONNECT_PROTOCOL, Protocol.TCP.name());
        } else {
            configMap.put(SOCKET_CONNECT_PROTOCOL, Protocol.UNIX_DOMAIN_SOCKET.name());
            configMap.put(UNIX_DOMAIN_SOCKET_PATH,"/dev/shm/om_benchmark.sock");
        }
        return FixClientFactory.loadConfig(name, configMap);
    }
}
