package com.piotrkafel.rate.limiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class FixedWindowRateLimiterDifferent<T> {

    private final int windowMaxSize;
    private final Map<T, Window> store = new ConcurrentHashMap<>();
    private final long windowSizeInMillies;

    public FixedWindowRateLimiterDifferent(int windowMaxSize, long timeValue, TimeUnit timeUnit) {
        if(windowMaxSize < 1) throw new IllegalArgumentException("Window size cannot be smaller than 1");
        this.windowMaxSize = windowMaxSize;
        this.windowSizeInMillies = timeUnit.toMillis(timeValue);
    }

    public boolean handleRequest(T key) {
        // for now I assume System.currentTimeMillis() returns monotonic clock (even though it does not)
        final long currentTimeMillis = System.currentTimeMillis();
        final Window window = store.computeIfAbsent(key, k -> new Window(currentTimeMillis));

        // synchronizing on window for better performance (instead of on the method)
        synchronized (window) {
            // check if we are in another window
            if(currentTimeMillis - window.getBeginOfWindowInMillis() > windowSizeInMillies) {
                // reset to new window
                window.resetWindowWithNewBegin(currentTimeMillis, 1);
                return true;
            }

            // check if number of requests + 1 is below threshold
            if(window.getNumberOfRequests() + 1 <= windowMaxSize) {
                window.increaseNumberOfRequests();
                return true;
            }

            return false;
        }
    }

    public class Window {

        private int numberOfRequests;

        private long beginOfWindowInMillis;

        public Window(long beginOfWindowInMillis) {
            resetWindowWithNewBegin(beginOfWindowInMillis, 0);
        }

        public int getNumberOfRequests() {
            return numberOfRequests;
        }

        public long getBeginOfWindowInMillis() {
            return beginOfWindowInMillis;
        }

        public void resetWindowWithNewBegin(long beginOfWindowInMillis, int numberOfRequests) {
            this.beginOfWindowInMillis = beginOfWindowInMillis;
            this.numberOfRequests = numberOfRequests;
        }

        public void increaseNumberOfRequests() {
            this.numberOfRequests++;
        }
    }
}
