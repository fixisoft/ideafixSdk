/*
 * Copyright (c) Pierre-Yves Peton 2024.
 * All rights reserved
 */

package com.fixisoft.fix.example.client;

import com.fixisoft.interfaces.fix.fields.*;
import com.fixisoft.interfaces.fix.message.IMessage;
import com.fixisoft.interfaces.fix.message.ImmutableMessage;
import com.fixisoft.interfaces.fix.session.IChannelContext;
import com.fixisoft.interfaces.fix.session.IFixIncomingHandler;
import com.fixisoft.util.TimeBasedUniqueIdSequence;
import io.netty.util.AsciiString;

import java.util.function.Supplier;

import static com.fixisoft.interfaces.fix.fields.MsgType.ORDER_SINGLE;


public final class OMBenchmarkClientHandler implements IFixIncomingHandler<IMessage> {

    private static final AsciiString MSFT = AsciiString.cached("MSFT");
    private static final AsciiString EXECUTION_REPORT = AsciiString.cached(MsgType.EXECUTION_REPORT);
    private IChannelContext<IMessage> ctx;
    private Supplier<AsciiString> sequence;
    private Supplier<IMessage> fastSupplier;
    private Supplier<IMessage> slowSupplier;

    @Override
    public void close() {
    }

    private void fillSingleNewOrder() {
        try {
            IMessage m;
            if ((m = fastSupplier.get()) == null) {
                if ((m = fastSupplier.get()) == null) {
                    m = slowSupplier.get();
                }
            }
            m.set(OrdType.FIELD, OrdType.LIMIT);
            m.set(Price.FIELD, ctx.decimal(100.122));
            m.set(Side.FIELD, Side.BUY);
            m.set(OrderQty.FIELD, ctx.decimal(400.50));
            m.set(Symbol.FIELD, MSFT);
            m.set(ClOrdID.FIELD, sequence.get());
            m.set(TransactTime.FIELD, ctx.nowUTC());
            ctx.sendAndFlush(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void onLogon(final IChannelContext<IMessage> ctx) {
        this.fastSupplier = ctx.getSupplier(ORDER_SINGLE);
        this.slowSupplier = ctx.getSupplier(ORDER_SINGLE);
        this.ctx = ctx;
        this.sequence = ctx.asyncSupplier(new TimeBasedUniqueIdSequence());
        fillSingleNewOrder();
    }

    @Override
    public void onLogout(final IChannelContext<IMessage> ctx) {

    }

    @Override
    public void onMessage(final ImmutableMessage incoming, final IChannelContext<IMessage> ctx) {
        if (EXECUTION_REPORT.equals(incoming.getType()) && incoming.getChar(OrdStatus.FIELD) == OrdStatus.FILLED)
            fillSingleNewOrder();
    }

}
