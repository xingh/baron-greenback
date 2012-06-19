package com.googlecode.barongreenback.crawler;

import com.googlecode.totallylazy.Pair;
import com.googlecode.utterlyidle.Response;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class RetryQueue implements StatusMonitor {
    public final BlockingQueue<Pair<HttpDataSource, Response>> value;

    public RetryQueue() {
        this.value = new LinkedBlockingDeque<Pair<HttpDataSource, Response>>();
    }

    @Override
    public String name() {
        return "Retry Queue";
    }

    @Override
    public int activeThreads() {
        return 0;
    }

    @Override
    public int size() {
        return value.size();
    }
}
