/*
 * Copyright (c) Pierre-Yves Peton 2023.
 * All rights reserved
 */

package com.fixisoft.fix.example.client;

import com.fixisoft.interfaces.fix.fields.MDReqID;
import com.fixisoft.interfaces.fix.fields.Symbol;
import com.fixisoft.interfaces.fix.message.IMessage;
import com.fixisoft.interfaces.fix.message.ImmutableMessage;
import com.fixisoft.interfaces.fix.session.IChannelContext;
import com.fixisoft.interfaces.fix.session.IFixIncomingHandler;
import com.fixisoft.util.TimeBasedUniqueIdSequence;
import io.netty.util.AsciiString;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import static com.fixisoft.interfaces.fix.fields.MsgType.MARKET_DATA_REQUEST;


public final class TestMDClientIncomingHandler implements IFixIncomingHandler<IMessage> {

    private static final List<AsciiString> TICKERS = List.of(AsciiString.cached("MSFT"), AsciiString.cached("GOOG"), AsciiString.cached("FB"));

    private static final List<Character> MD_ENTRY_TYPES = List.of(MDEntryType.Bid, MDEntryType.Offer);

    private IChannelContext<IMessage> ctx;
    private ScheduledFuture<?> future;
    private Supplier<AsciiString> sequence;
    private Supplier<IMessage> supplier;

    @Override
    public void close() {
        if (future != null) future.cancel(true);
    }


    private IMessage makeSubscriptionMessage() {
        final IMessage m = supplier.get();
        m.set(MDReqID.FIELD, sequence.get());
        m.set(SubscriptionRequestType.FIELD, SubscriptionRequestType.SNAPSHOT);
        m.set(MarketDepth.FIELD, MarketDepth.FULL_BOOK);
        for (int i = 0; i < MD_ENTRY_TYPES.size(); i++)
            m.set(NoMDEntryTypes.FIELD, i, MDEntryType.FIELD, MD_ENTRY_TYPES.get(i));
        for (int i = 0; i < TICKERS.size(); i++)
            m.set(NoRelatedSym.FIELD, i, Symbol.FIELD, TICKERS.get(i));
        return m;
    }


    @Override
    public void onLogon(final IChannelContext<IMessage> ctx) {
        this.supplier = ctx.getSupplier(MARKET_DATA_REQUEST);
        this.ctx = ctx;
        this.sequence = ctx.asyncSupplier(new TimeBasedUniqueIdSequence());
        ctx.sendAndFlush(makeSubscriptionMessage());
    }

    @Override
    public void onLogout(final IChannelContext<IMessage> ctx) {

    }

    @Override
    public void onMessage(final ImmutableMessage incoming, final IChannelContext<IMessage> ctx) {

    }

}
