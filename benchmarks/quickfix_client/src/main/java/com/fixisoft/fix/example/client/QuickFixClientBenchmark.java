package com.fixisoft.fix.example.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import javax.management.JMException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public final class QuickFixClientBenchmark {
    private final static Logger log = LoggerFactory.getLogger(QuickFixClientBenchmark.class);
    private final SocketInitiator initiator;

    public QuickFixClientBenchmark(final SessionSettings settings) throws ConfigError, FieldConvertError, JMException {
        final QuickFixClientApplication client = new QuickFixClientApplication(settings);
        final MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        final LogFactory logFactory = new SLF4JLogFactory(settings);
        final MessageFactory messageFactory = new DefaultMessageFactory();
        initiator = new SocketInitiator(client, messageStoreFactory, settings, logFactory, messageFactory);
    }

    private static InputStream getSettingsInputStream(String[] args) throws FileNotFoundException {
        InputStream inputStream = null;
        if (args.length == 0)
            inputStream = QuickFixClientBenchmark.class.getClassLoader().getResourceAsStream("quickfix_client.cfg");
        else if (args.length == 1) inputStream = new FileInputStream(args[0]);
        if (inputStream == null) {
            log.info("usage: " + QuickFixClientBenchmark.class.getName() + " [configFile].");
            System.exit(1);
        }
        return inputStream;
    }


    public static void main(String[] args) {
        try {
            InputStream inputStream = getSettingsInputStream(args);
            SessionSettings settings = new SessionSettings(inputStream);
            inputStream.close();
            QuickFixClientBenchmark client = new QuickFixClientBenchmark(settings);
            Runtime.getRuntime().addShutdownHook(new Thread(client::stop));
            client.start();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void start() throws RuntimeError, ConfigError, InterruptedException {
        initiator.start();
        synchronized (this) {
            wait();
        }
    }

    private void stop() {
        synchronized (this) {
            notifyAll();
        }
        initiator.stop();
    }
}
