/*
 * Copyright (c) Pierre-Yves Peton 2023.
 * All rights reserved
 */

package com.fixisoft.fix.example.server;

import com.fixisoft.interfaces.fix.InvalidFixException;
import com.fixisoft.interfaces.fix.fields.*;
import com.fixisoft.interfaces.fix.message.IMessage;
import com.fixisoft.interfaces.fix.message.ImmutableMessage;
import com.fixisoft.interfaces.fix.session.IChannelContext;
import com.fixisoft.interfaces.fix.session.IFixIncomingHandler;
import com.fixisoft.util.TimeBasedUniqueIdSequence;
import io.netty.util.AsciiString;

import java.util.function.Supplier;

import static com.fixisoft.interfaces.fix.fields.MsgType.EXECUTION_REPORT;


public final class TestServerIncomingHandler implements IFixIncomingHandler<IMessage> {

    private static final AsciiString ORDER_SINGLE = AsciiString.of(MsgType.ORDER_SINGLE);
    private Supplier<AsciiString> execIds;
    private Supplier<IMessage> fastSupplier;
    private Supplier<AsciiString> orderIds;
    private Supplier<IMessage> slowSupplier;

    private IMessage fillExecutionReport(final IChannelContext<IMessage> ctx, final char status, final Object symbol, final Object orderQty, final Object lastPx, final Object cumQty, final Object avgPx)  {
        final IMessage m = ctx.makeMessageOpt(fastSupplier, slowSupplier);
        m.set(OrderID.FIELD, orderIds.get());
        m.set(ExecID.FIELD, execIds.get());
        m.set(ExecType.FIELD, ExecType.FILL);
        m.set(OrdStatus.FIELD, status);
        m.set(Symbol.FIELD, symbol);
        m.set(OrderQty.FIELD, orderQty);
        m.set(LastPx.FIELD, lastPx);
        m.set(CumQty.FIELD, cumQty);
        m.set(AvgPx.FIELD, avgPx);
        return m;
    }

    @Override
    public void onLogon(final IChannelContext<IMessage> ctx) {
        this.fastSupplier = ctx.getSupplier(EXECUTION_REPORT);
        this.slowSupplier = ctx.getSlowSupplier(EXECUTION_REPORT);
        this.execIds = ctx.asyncSupplier(new TimeBasedUniqueIdSequence());
        this.orderIds = ctx.asyncSupplier(new TimeBasedUniqueIdSequence());
    }

    @Override
    public void onLogout(final IChannelContext<IMessage> ctx) {
    }

    @Override
    public void onMessage(final ImmutableMessage incoming, final IChannelContext<IMessage> ctx)  {
        if (ORDER_SINGLE.equals(incoming.getType())) {
            final Object symbol = incoming.get(Symbol.FIELD);
            final Object orderQty = incoming.get(OrderQty.FIELD);
            final Object lastPx = incoming.get(Price.FIELD);
            ctx.sendAndFlush(fillExecutionReport(ctx, OrdStatus.NEW, symbol, orderQty, lastPx, orderQty, lastPx), fillExecutionReport(ctx, OrdStatus.FILLED, symbol, orderQty, lastPx, orderQty, lastPx));
        }
    }
}
