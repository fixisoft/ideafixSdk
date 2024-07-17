package com.fixisoft.fix.example.server;

import com.fixisoft.interfaces.fix.fields.fix44.MDReqID;
import com.fixisoft.interfaces.fix.fields.fix44.MsgType;
import com.fixisoft.interfaces.fix.message.IMessage;
import com.fixisoft.interfaces.fix.session.IChannelContext;
import com.fixisoft.util.generator.RandomMarketDataGenerator;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.function.Supplier;

public final class MarketDataRunnable implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MarketDataRunnable.class);

    private final AsciiString[] symbols;

    private final Supplier<IMessage> supplier;

    private final AsciiString requestId;

    private final IChannelContext<IMessage> ctx;

    private final RandomMarketDataGenerator[] marketDataGenerators;

    private volatile boolean running = true;

    private int i = 0;

    public MarketDataRunnable(final AsciiString requestId, final AsciiString[] symbols, final IChannelContext<IMessage> ctx) {
        this.requestId = requestId;
        this.symbols = symbols;
        this.ctx = ctx;
        this.supplier = ctx.getSupplier(MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH);
        this.marketDataGenerators = new RandomMarketDataGenerator[symbols.length];
        for (int i = 0; i < marketDataGenerators.length; i++) {
            this.marketDataGenerators[i] = new RandomMarketDataGenerator(symbols[i], 100.0, 0.001, 1000000, 0.0001, 5, 0.8, 1000);
        }
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        if (!running || ctx.isClosed()) return;
        if (ctx.isWritable()) {
            try {
                for (; i < symbols.length - 1; i++) {
                    final IMessage snapshot = this.supplier.get();
                    if (snapshot==null)// run until the pool is empty
                        return;
                    this.marketDataGenerators[i].accept(snapshot);
                    snapshot.set(MDReqID.FIELD, requestId);
                    ctx.send(snapshot);
                }
                final IMessage snapshot = this.supplier.get();
                if (snapshot==null)// run until the pool is empty
                    return;
                this.marketDataGenerators[symbols.length - 1].accept(snapshot);
                snapshot.set(MDReqID.FIELD, requestId);
                ctx.sendAndFlush(snapshot);
            } catch (final Exception ex) {
                log.error("could not send market data for symbols " + Arrays.toString(symbols), ex);
            } finally {
                i = 0;
            }
        } else {
            log.warn("[SLOW-CONSUMER] channel is saturated, skipping update ...");
        }
    }
}
