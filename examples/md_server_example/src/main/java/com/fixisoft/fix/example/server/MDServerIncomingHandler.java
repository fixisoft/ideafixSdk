/*
 * Copyright (c) Pierre-Yves Peton 2023.
 * All rights reserved
 */

package com.fixisoft.fix.example.server;

import com.fixisoft.interfaces.fix.InvalidFixException;
import com.fixisoft.interfaces.fix.fields.*;
import com.fixisoft.interfaces.fix.message.IGroup;
import com.fixisoft.interfaces.fix.message.IMessage;
import com.fixisoft.interfaces.fix.message.ImmutableMessage;
import com.fixisoft.interfaces.fix.session.IChannelContext;
import com.fixisoft.interfaces.fix.session.IFixIncomingHandler;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.fixisoft.interfaces.fix.fields.MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH;


public final class MDServerIncomingHandler implements IFixIncomingHandler<IMessage> {

    private static final AsciiString MARKET_DATA_REQUEST = AsciiString.of(MsgType.MARKET_DATA_REQUEST);

    private static final AsciiString USD = AsciiString.of("USD");

    private static final Logger log = LoggerFactory.getLogger(MDServerIncomingHandler.class);

    private IChannelContext<IMessage> ctx;

    private int entryCount;

    private char[] entryTypes;

    private Supplier<IMessage> fastSupplier;

    private ScheduledFuture<?> future;

    private AsciiString requestId;

    private Supplier<IMessage> slowSupplier;

    private int symbolCount;

    private AsciiString[] symbols;

    private boolean running = false;

    @Override
    public void onDisconnection() {
        this.running = false;
        this.future.cancel(true);
        this.ctx = null;
        this.slowSupplier = null;
        this.fastSupplier = null;
        this.future = null;
        this.entryCount = 0;
        this.entryTypes = null;
        this.running =false;
        symbolCount = 0;
    }

    @Override
    public void onLogon(final IChannelContext<IMessage> ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onLogout(final IChannelContext<IMessage> ctx) {

    }

    @Override
    public void onMessage(final ImmutableMessage incoming, final IChannelContext<IMessage> ctx) {
        if (MARKET_DATA_REQUEST.equals(incoming.getType())) {
            this.fastSupplier = ctx.getSupplier(MARKET_DATA_SNAPSHOT_FULL_REFRESH);
            this.slowSupplier = ctx.getSlowSupplier(MARKET_DATA_SNAPSHOT_FULL_REFRESH);
            this.entryCount = incoming.getIntOr(NoMDEntryTypes.FIELD, 0);
            this.symbolCount = incoming.getIntOr(NoRelatedSym.FIELD, 0);
            this.requestId = (AsciiString) incoming.get(MDReqID.FIELD);
            this.entryTypes = new char[entryCount];
            this.symbols = new AsciiString[symbolCount];
            for (int k = 0; k < entryCount; k++)
                entryTypes[k] = ((Character) incoming.getGroup(NoMDEntryTypes.FIELD).get(k, MDEntryType.FIELD));
            for (int k = 0; k < symbolCount; k++)
                symbols[k] = ((AsciiString) incoming.getGroup(NoRelatedSym.FIELD).get(k, Symbol.FIELD));
            this.running = true;
            this.future = this.ctx.scheduleWithFixedDelay(this::sendMarketData, 5000000, 300, TimeUnit.MICROSECONDS);
        }
    }

    private void sendMarketData() {
        try {
            if(!running)
                return;
            final int len = symbols.length;
            if (len > 0) {
                for (int i = 0; i < len - 1; i++) {
                    IMessage snapshot = fastSupplier.get();
                    if (snapshot==null) snapshot = slowSupplier.get();
                    snapshot.set(MDReqID.FIELD, requestId);
                    snapshot.set(Symbol.FIELD, symbols[i]);
                    final IGroup g = snapshot.getGroup(NoMDEntries.FIELD);
                    for (int k = 0; k < entryCount; k++) {
                        g.set(k, MDEntryType.FIELD, entryTypes[k]);
                        g.set(k, MDEntryPx.FIELD, ctx.decimal(100.0 + k));
                        g.set(k, MDEntrySize.FIELD, ctx.decimal(200.0 + k));
                        g.set(k, Currency.FIELD, USD);
                    }
                    ctx.send(snapshot);
                }
                IMessage snapshot = fastSupplier.get();
                if (snapshot==null) snapshot = slowSupplier.get();
                snapshot.set(MDReqID.FIELD, requestId);
                snapshot.set(Symbol.FIELD, symbols[len - 1]);
                final IGroup g = snapshot.getGroup(NoMDEntries.FIELD);
                for (int k = 0; k < entryCount; k++) {
                    g.set(k, MDEntryType.FIELD, entryTypes[k]);
                    g.set(k, MDEntryPx.FIELD, ctx.decimal(100.0 + k));
                    g.set(k, MDEntrySize.FIELD, ctx.decimal(200.0 + k));
                    g.set(k, Currency.FIELD, USD);
                }
                ctx.sendAndFlush(snapshot);
            }
        } catch (final Throwable ex) {
            log.error("could not send market data", ex);
        }
    }

}
