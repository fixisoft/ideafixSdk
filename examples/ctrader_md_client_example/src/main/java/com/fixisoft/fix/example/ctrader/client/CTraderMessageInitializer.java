package com.fixisoft.fix.example.ctrader.client;

import com.fixisoft.interfaces.fix.InvalidFixException;
import com.fixisoft.interfaces.fix.fields.fix44.Password;
import com.fixisoft.interfaces.fix.fields.fix44.SenderSubID;
import com.fixisoft.interfaces.fix.fields.fix44.TargetSubID;
import com.fixisoft.interfaces.fix.fields.fix44.Username;
import com.fixisoft.interfaces.fix.message.IMessage;
import com.fixisoft.interfaces.fix.session.IFixMessageInitializer;
import io.netty.util.AsciiString;

public final class CTraderMessageInitializer implements IFixMessageInitializer {

    public static final AsciiString PASSWORD = AsciiString.cached("your_password");

    public static final AsciiString QUOTE = AsciiString.cached("QUOTE");

    public static final AsciiString USERNAME = AsciiString.cached("your_username");

    @Override
    public void init(final IMessage message) {

    }

    @Override
    public void initHeartbeat(final IMessage message) {

    }

    @Override
    public void initLogon(final IMessage message)  {
        message.set(Username.FIELD, USERNAME);
        message.set(Password.FIELD, PASSWORD);
        message.set(SenderSubID.FIELD, QUOTE);
        message.set(TargetSubID.FIELD, QUOTE);
    }

    @Override
    public void initLogout(final IMessage message) {

    }
}
