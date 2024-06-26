/*
 * Copyright (c) Pierre-Yves Peton 2023.
 * All rights reserved
 */

package com.fixisoft.fix.example.client;

import com.fixisoft.interfaces.fix.fields.*;
import com.fixisoft.interfaces.fix.message.IMessage;
import com.fixisoft.interfaces.fix.message.ImmutableMessage;
import com.fixisoft.interfaces.fix.session.IChannelContext;
import com.fixisoft.interfaces.fix.session.IFixIncomingHandler;
import com.fixisoft.util.TimeBasedUniqueIdSequenceDirect;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.fixisoft.interfaces.fix.fields.MsgType.ORDER_SINGLE;


public final class OMClientIncomingDirectHandler implements IFixIncomingHandler<IMessage> {

    private static final AsciiString MSFT = AsciiString.cached("MSFT");
    private IChannelContext<IMessage> ctx;
    private ScheduledFuture<?> future;
    private Supplier<ByteBuf> sequence;
    private Supplier<IMessage> supplier;

    @Override
    public void close() {
        if (future != null) future.cancel(true);
    }

    @Override
    public void onDisconnection() {
        if (future != null) future.cancel(true);
    }

    private void fillSingleNewOrder() {
        try {
            for (int i = 0; i < 3; i++) {
                final IMessage m = supplier.get();
                if (m == null) return;
                m.set(OrdType.FIELD, OrdType.LIMIT);
                m.set(Price.FIELD, ctx.decimal(100.122));
                m.set(Side.FIELD, Side.BUY);
                m.set(OrderQty.FIELD, ctx.decimal(400.50));
                m.set(Symbol.FIELD, MSFT);
                m.setDirect(ClOrdID.FIELD, sequence.get());
                m.setDirect(TransactTime.FIELD, ctx.nowUTCDirect());
                ctx.send(m);
            }
            final IMessage m = supplier.get();
            if (m == null) return;
            m.set(OrdType.FIELD, OrdType.LIMIT);
            m.set(Price.FIELD, ctx.decimal(100.122));
            m.set(Side.FIELD, Side.BUY);
            m.set(OrderQty.FIELD,  ctx.decimal(400.50));
            m.set(Symbol.FIELD, MSFT);
            m.setDirect(ClOrdID.FIELD, sequence.get());
            m.setDirect(TransactTime.FIELD, ctx.nowUTCDirect());
            ctx.sendAndFlush(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void onLogon(final IChannelContext<IMessage> ctx) {
        this.supplier = ctx.getSupplier(ORDER_SINGLE);
        this.ctx = ctx;
        this.sequence = ctx.asyncSupplier(new TimeBasedUniqueIdSequenceDirect());
        this.future = ctx.scheduleWithFixedDelay(this::fillSingleNewOrder, 5000, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onLogout(final IChannelContext<IMessage> ctx) {

    }

    @Override
    public void onMessage(final ImmutableMessage incoming, final IChannelContext<IMessage> ctx) {

    }

}
