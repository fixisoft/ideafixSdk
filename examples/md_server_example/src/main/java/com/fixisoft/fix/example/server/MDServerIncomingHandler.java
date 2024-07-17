/*
 * Copyright (c) Pierre-Yves Peton 2023.
 * All rights reserved
 */

package com.fixisoft.fix.example.server;

import com.fixisoft.interfaces.fix.fields.fix44.MDReqID;
import com.fixisoft.interfaces.fix.fields.fix44.MsgType;
import com.fixisoft.interfaces.fix.fields.fix44.NoRelatedSym;
import com.fixisoft.interfaces.fix.fields.fix44.Symbol;
import com.fixisoft.interfaces.fix.message.IMessage;
import com.fixisoft.interfaces.fix.message.ImmutableMessage;
import com.fixisoft.interfaces.fix.session.IChannelContext;
import com.fixisoft.interfaces.fix.session.IFixIncomingHandler;
import io.netty.util.AsciiString;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public final class MDServerIncomingHandler implements IFixIncomingHandler<IMessage> {

    private static final AsciiString MARKET_DATA_REQUEST = AsciiString.of(MsgType.MARKET_DATA_REQUEST);

    private IChannelContext<IMessage> ctx;

    private MarketDataRunnable marketDataRunnable;

    private ScheduledFuture<?> future;

    @Override
    public void onDisconnection() {
        stopMarketData();
        this.ctx = null;
    }

    private void stopMarketData() {
        if (this.marketDataRunnable != null) {
            this.marketDataRunnable.stop();
            this.marketDataRunnable = null;
        }
        if (this.future != null) {
            this.future.cancel(true);
            this.future = null;
        }
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
            stopMarketData();//only 1 request at a time
            final int symbolCount = incoming.getIntOr(NoRelatedSym.FIELD, 0);
            final AsciiString[] symbols = new AsciiString[symbolCount];
            for (int k = 0; k < symbolCount; k++)
                symbols[k] = ((AsciiString) incoming.getGroup(NoRelatedSym.FIELD).get(k, Symbol.FIELD));
            this.marketDataRunnable = new MarketDataRunnable((AsciiString) incoming.get(MDReqID.FIELD), symbols, ctx);
            this.future = this.ctx.scheduleWithFixedDelay(this.marketDataRunnable, 5000000, 300, TimeUnit.MICROSECONDS);
        }
    }


}
