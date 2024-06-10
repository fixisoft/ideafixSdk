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

package com.fixisoft.fix.example.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;

public final class QuickFixClientApplication extends quickfix.MessageCracker implements quickfix.Application {
    private static final OrdType ORDER_TYPE = new OrdType(OrdType.LIMIT);
    private static final Price PRICE = new Price(100.122);
    private static final Side SIDE = new Side(Side.BUY);
    private static final OrderQty ORDER_QTY = new OrderQty(400.50);
    private static final Symbol SYMBOL = new Symbol("MSFT");
    private static final Logger log = LoggerFactory.getLogger(QuickFixClientApplication.class);
    private int clOrderId = 0;

    public QuickFixClientApplication(final SessionSettings settings) {
    }

    public void fromAdmin(Message message, SessionID sessionID) {
    }

    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }

    public ClOrdID genClOrdID() {
        return new ClOrdID(Integer.toString(++clOrderId));
    }

    public void onCreate(final SessionID sessionID) {
    }

    public void onLogon(final SessionID sessionID) {
        sendOrder(sessionID);
    }

    public void onLogout(final SessionID sessionID) {
    }

    public void onMessage(quickfix.fix44.ExecutionReport executionReport, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        try {
            if (executionReport.getChar(OrdStatus.FIELD) == OrdStatus.FILLED) sendOrder(sessionID);
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    private void sendOrder(final SessionID sessionID) {
        final NewOrderSingle order = new NewOrderSingle(genClOrdID(), SIDE, new TransactTime(), ORDER_TYPE);
        order.set(PRICE);
        order.set(ORDER_QTY);
        order.set(SYMBOL);
        sendMessage(sessionID, order);
    }


    private void sendMessage(final SessionID sessionID, final Message message) {
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
