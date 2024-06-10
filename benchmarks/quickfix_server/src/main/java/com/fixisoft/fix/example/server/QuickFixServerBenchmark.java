package com.fixisoft.fix.example.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider.TemplateMapping;

import javax.management.JMException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;

import static quickfix.Acceptor.*;

public final class QuickFixServerBenchmark {
    private final static Logger log = LoggerFactory.getLogger(QuickFixServerBenchmark.class);
    private final SocketAcceptor acceptor;
    private final Map<InetSocketAddress, List<TemplateMapping>> dynamicSessionMappings = new HashMap<>();


    public QuickFixServerBenchmark(SessionSettings settings) throws ConfigError, FieldConvertError, JMException {
        QuickFixServerApplication server = new QuickFixServerApplication(settings);
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new SLF4JLogFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();
        acceptor = new SocketAcceptor(server, messageStoreFactory, settings, logFactory, messageFactory);
        configureDynamicSessions(settings, server, messageStoreFactory, logFactory, messageFactory);
    }

    private static InputStream getSettingsInputStream(String[] args) throws FileNotFoundException {
        InputStream inputStream = null;
        if (args.length == 0)
            inputStream = QuickFixServerBenchmark.class.getClassLoader().getResourceAsStream("quickfix_server.cfg");
        else if (args.length == 1) inputStream = new FileInputStream(args[0]);
        if (inputStream == null) {
            log.info("usage: " + QuickFixServerBenchmark.class.getName() + " [configFile].");
            System.exit(1);
        }
        return inputStream;
    }


    public static void main(String[] args) {
        try {
            InputStream inputStream = getSettingsInputStream(args);
            SessionSettings settings = new SessionSettings(inputStream);
            inputStream.close();
            QuickFixServerBenchmark quickFixServerBenchmark = new QuickFixServerBenchmark(settings);
            quickFixServerBenchmark.start();
            Runtime.getRuntime().addShutdownHook(new Thread(quickFixServerBenchmark::stop));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void configureDynamicSessions(SessionSettings settings, QuickFixServerApplication quickFixServerApplication, MessageStoreFactory messageStoreFactory, LogFactory logFactory, MessageFactory messageFactory) throws ConfigError, FieldConvertError {
        //
        // If a session template is detected in the settings, then
        // set up a dynamic session provider.
        //
        Iterator<SessionID> sectionIterator = settings.sectionIterator();
        while (sectionIterator.hasNext()) {
            SessionID sessionID = sectionIterator.next();
            if (isSessionTemplate(settings, sessionID)) {
                InetSocketAddress address = getAcceptorSocketAddress(settings, sessionID);
                getMappings(address).add(new TemplateMapping(sessionID, sessionID));
            }
        }
        for (Map.Entry<InetSocketAddress, List<TemplateMapping>> entry : dynamicSessionMappings.entrySet())
            acceptor.setSessionProvider(entry.getKey(), new DynamicAcceptorSessionProvider(settings, entry.getValue(), quickFixServerApplication, messageStoreFactory, logFactory, messageFactory));
    }

    private InetSocketAddress getAcceptorSocketAddress(SessionSettings settings, SessionID sessionID) throws ConfigError, FieldConvertError {
        String acceptorHost = "0.0.0.0";
        if (settings.isSetting(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS))
            acceptorHost = settings.getString(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS);
        return new InetSocketAddress(acceptorHost, (int) settings.getLong(sessionID, SETTING_SOCKET_ACCEPT_PORT));
    }

    private List<TemplateMapping> getMappings(InetSocketAddress address) {
        return dynamicSessionMappings.computeIfAbsent(address, k -> new ArrayList<>());
    }

    private boolean isSessionTemplate(SessionSettings settings, SessionID sessionID) throws ConfigError, FieldConvertError {
        return settings.isSetting(sessionID, SETTING_ACCEPTOR_TEMPLATE) && settings.getBool(sessionID, SETTING_ACCEPTOR_TEMPLATE);
    }

    private void start() throws RuntimeError, ConfigError {
        acceptor.start();
    }

    private void stop() {
        acceptor.stop();
    }
}
