/*
 * Copyright (c) Pierre-Yves Peton 2024.
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
import com.fixisoft.util.types.FixDecimal;
import io.netty.util.AsciiString;

import java.util.function.Supplier;

import static com.fixisoft.interfaces.fix.fields.MsgType.EXECUTION_REPORT;


public final class OMBenchmarkServerHandler implements IFixIncomingHandler<IMessage> {

    private static final AsciiString ORDER_SINGLE = AsciiString.cached(MsgType.ORDER_SINGLE);
    private Supplier<AsciiString> execIds;
    private Supplier<IMessage> fastSupplier;
    private Supplier<AsciiString> orderIds;
    private Supplier<IMessage> slowSupplier;

    private IMessage fillExecutionReport(final IChannelContext<IMessage> ctx, final char status, final AsciiString symbol, final FixDecimal qty, final FixDecimal price) throws InvalidFixException {
        final IMessage m = ctx.makeMessageOpt(fastSupplier, slowSupplier);
        m.set(OrderID.FIELD, orderIds.get());
        m.set(ExecID.FIELD, execIds.get());
        m.set(ExecType.FIELD, ExecType.FILL);
        m.set(OrdStatus.FIELD, status);
        m.set(Symbol.FIELD, symbol);
        m.set(OrderQty.FIELD, qty);
        m.set(CumQty.FIELD, qty);
        m.set(LastPx.FIELD, price);
        m.set(AvgPx.FIELD, price);
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
    public void onMessage(final ImmutableMessage incoming, final IChannelContext<IMessage> ctx) {
        if (ORDER_SINGLE.equals(incoming.getType())) {
            try {
                final AsciiString symbol = incoming.getString(Symbol.FIELD);
                final FixDecimal qty = incoming.getDecimal(OrderQty.FIELD);
                final FixDecimal price = incoming.getDecimal(Price.FIELD);
                ctx.sendAndFlush(fillExecutionReport(ctx, OrdStatus.NEW, symbol, qty, price), fillExecutionReport(ctx, OrdStatus.FILLED, symbol, qty, price));
            } catch (InvalidFixException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
