/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package com.fixisoft.fix.example.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;

public final class QuickFixServerApplication extends quickfix.MessageCracker implements quickfix.Application {
    private static final String ALWAYS_FILL_LIMIT_KEY = "AlwaysFillLimitOrders";
    private static final String DEFAULT_MARKET_PRICE_KEY = "DefaultMarketPrice";
    private static final Logger log = LoggerFactory.getLogger(QuickFixServerApplication.class);
    private static final ExecType EXEC_TYPE_NEW = new ExecType(ExecType.NEW);
    private static final OrdStatus ORD_STATUS_NEW = new OrdStatus(OrdStatus.NEW);
    private static final OrdStatus ORD_STATUS_FILLED = new OrdStatus(OrdStatus.FILLED);
    private static final LastPx LAST_PX_ZERO = new LastPx(0);
    private static final CumQty CUM_QTY_ZERO = new CumQty(0);
    private static final AvgPx AVG_PX_ZERO = new AvgPx(0);
    private final boolean alwaysFillLimitOrders;
    private int m_execID = 0;
    private int m_orderID = 0;
    private MarketDataProvider marketDataProvider;

    public QuickFixServerApplication(SessionSettings settings) throws ConfigError, FieldConvertError {
        initializeMarketDataProvider(settings);
        alwaysFillLimitOrders = settings.isSetting(ALWAYS_FILL_LIMIT_KEY) && settings.getBool(ALWAYS_FILL_LIMIT_KEY);
    }

    public void fromAdmin(final Message message, final SessionID sessionID) {
    }

    public void fromApp(final Message message, final SessionID sessionID) throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }

    public ExecID genExecID() {
        return new ExecID(Integer.toString(++m_execID));
    }

    public OrderID genOrderID() {
        return new OrderID(Integer.toString(++m_orderID));
    }

    private Price getPrice(final Message message) throws FieldNotFound {
        final Price price;
        if (message.getChar(OrdType.FIELD) == OrdType.LIMIT && alwaysFillLimitOrders) {
            price = new Price(message.getDouble(Price.FIELD));
        } else {
            if (marketDataProvider == null)
                throw new RuntimeException("No market data provider specified for market order");
            final char side = message.getChar(Side.FIELD);
            if (side == Side.BUY) price = new Price(marketDataProvider.getAsk(message.getString(Symbol.FIELD)));
            else if (side == Side.SELL || side == Side.SELL_SHORT)
                price = new Price(marketDataProvider.getBid(message.getString(Symbol.FIELD)));
            else throw new RuntimeException("Invalid order side: " + side);
        }
        return price;
    }

    private void initializeMarketDataProvider(SessionSettings settings) throws ConfigError, FieldConvertError {
        if (settings.isSetting(DEFAULT_MARKET_PRICE_KEY)) {
            if (marketDataProvider == null) {
                final double defaultMarketPrice = settings.getDouble(DEFAULT_MARKET_PRICE_KEY);
                marketDataProvider = new MarketDataProvider() {
                    public double getAsk(String symbol) {
                        return defaultMarketPrice;
                    }

                    public double getBid(String symbol) {
                        return defaultMarketPrice;
                    }
                };
            } else {
                log.warn("Ignoring {} since provider is already defined.", DEFAULT_MARKET_PRICE_KEY);
            }
        }
    }


    public void onCreate(final SessionID sessionID) {

    }

    public void onLogon(final SessionID sessionID) {
    }

    public void onLogout(final SessionID sessionID) {
    }

    public void onMessage(final quickfix.fix44.NewOrderSingle order, final SessionID sessionID) throws FieldNotFound {
        try {
            final OrderQty orderQty = order.getOrderQty();
            final Price price = getPrice(order);
            final quickfix.fix44.ExecutionReport accept = new quickfix.fix44.ExecutionReport();
            accept.set(genOrderID());
            accept.set(genExecID());
            accept.set(EXEC_TYPE_NEW);
            accept.set(ORD_STATUS_NEW);
            accept.set(order.getSymbol());
            accept.set(orderQty);
            accept.set(LAST_PX_ZERO);
            accept.set(CUM_QTY_ZERO);
            accept.set(AVG_PX_ZERO);
            sendMessage(sessionID, accept);

            final quickfix.fix44.ExecutionReport fill = new quickfix.fix44.ExecutionReport();
            fill.set(genOrderID());
            fill.set(genExecID());
            fill.set(EXEC_TYPE_NEW);
            fill.set(ORD_STATUS_FILLED);
            fill.set(order.getSymbol());
            fill.set(orderQty);
            fill.set(new LastPx(price.getValue()));
            fill.set(new CumQty(orderQty.getValue()));
            fill.set(new AvgPx(price.getValue()));
            sendMessage(sessionID, fill);
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }


    private void sendMessage(SessionID sessionID, Message message) {
        try {
            Session session = Session.lookupSession(sessionID);
            if (session == null) throw new SessionNotFound(sessionID.toString());
            session.send(message);
        } catch (SessionNotFound e) {
            log.error(e.getMessage(), e);
        }
    }

    public void toAdmin(final Message message, final SessionID sessionID) {
    }

    public void toApp(final Message message, final SessionID sessionID) {
    }


}
