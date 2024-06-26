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
import com.fixisoft.util.TimeBasedUniqueIdSequenceDirect;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;

import java.util.function.Supplier;

import static com.fixisoft.interfaces.fix.fields.MsgType.EXECUTION_REPORT;


public final class OMBenchmarkDirectServerHandler implements IFixIncomingHandler<IMessage> {

    private static final AsciiString ORDER_SINGLE = AsciiString.cached(MsgType.ORDER_SINGLE);
    private Supplier<ByteBuf> execIds;
    private Supplier<IMessage> fastSupplier;
    private Supplier<ByteBuf> orderIds;
    private Supplier<IMessage> slowSupplier;

    private IMessage fillExecutionReport(final IChannelContext<IMessage> ctx, final char status, final ImmutableMessage incoming) throws InvalidFixException {
        IMessage m;
        if ((m = fastSupplier.get()) == null) {
            if ((m = fastSupplier.get()) == null) {
                m = slowSupplier.get();
            }
        }
        m.setDirect(OrderID.FIELD, orderIds.get());
        m.setDirect(ExecID.FIELD, execIds.get());
        m.set(ExecType.FIELD, ExecType.FILL);
        m.set(OrdStatus.FIELD, status);
        incoming.transferTo(Symbol.FIELD, Symbol.FIELD, m);
        incoming.transferTo(OrderQty.FIELD, OrderQty.FIELD, m);
        incoming.transferTo(OrderQty.FIELD, CumQty.FIELD, m);
        incoming.transferTo(Price.FIELD, LastPx.FIELD, m);
        incoming.transferTo(Price.FIELD, AvgPx.FIELD, m);
        return m;
    }

    @Override
    public void onLogon(final IChannelContext<IMessage> ctx) {
        this.fastSupplier = ctx.getSupplier(EXECUTION_REPORT);
        this.slowSupplier = ctx.getSlowSupplier(EXECUTION_REPORT);
        this.execIds = ctx.asyncSupplier(new TimeBasedUniqueIdSequenceDirect());
        this.orderIds = ctx.asyncSupplier(new TimeBasedUniqueIdSequenceDirect());
    }

    @Override
    public void onLogout(final IChannelContext<IMessage> ctx) {
    }

    @Override
    public void onMessage(final ImmutableMessage incoming, final IChannelContext<IMessage> ctx) {
        if (ORDER_SINGLE.equals(incoming.getType())) {
            try {
                ctx.sendAndFlush(
                        fillExecutionReport(ctx, OrdStatus.NEW, incoming),
                        fillExecutionReport(ctx, OrdStatus.FILLED, incoming));
            } catch (InvalidFixException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
