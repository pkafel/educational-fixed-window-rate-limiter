package com.piotrkafel.rate.limiter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FixedWindowRateLimiterSimple<T> {

    private final int windowMaxCapacity;

    private final long windowSizeInNanos;

    private final IClock clock;

    private final Map<T, Window> store = new HashMap<>();

    public FixedWindowRateLimiterSimple(int windowMaxCapacity, long windowLength, TimeUnit windowLengthTimeUnit, IClock clock) {
        if(windowMaxCapacity < 1) throw new IllegalArgumentException("Window size cannot be smaller than 1");
        this.windowMaxCapacity = windowMaxCapacity;
        this.windowSizeInNanos = windowLengthTimeUnit.toNanos(windowLength);
        this.clock = clock;
    }

    public synchronized boolean handleRequest(T key) {
        // Its important to use monotonic clock here. System.currentTimeMillis is not recommended as with it  we can
        // observe time going backward.
        final long currentTimeNanos = clock.nanoTime();

        Window window = store.computeIfAbsent(key, k -> new Window(currentTimeNanos));

        // Check if we are in another window. If so lets reset the window.
        if(currentTimeNanos - window.getBeginOfWindowInNanos() > windowSizeInNanos) {
            window.resetWindowWithNewBegin(currentTimeNanos, 1);
            return true;
        }

        // Check if number of requests + 1 is below max allowed window capacity.
        if(window.getNumberOfRequests() + 1 <= windowMaxCapacity) {
            window.increaseNumberOfRequests();
            return true;
        }

        return false;
    }
}
