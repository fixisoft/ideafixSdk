package com.fixisoft.util.concurrent;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import org.apache.logging.log4j.core.async.AsyncWaitStrategyFactory;

public final class BusySpinWaitFactory implements AsyncWaitStrategyFactory {
    @Override
    public WaitStrategy createWaitStrategy() {
        return new BusySpinWaitStrategy();
    }
}
