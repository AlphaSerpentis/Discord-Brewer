package dev.alphaserpentis.bots.brewer.executor;

import io.reactivex.rxjava3.annotations.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomExecutors {

    /**
     * Creates a new cached thread pool with the given core pool size.
     * @param corePoolSize The number of threads to keep in the pool, even if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @return The newly created thread pool
     * @see java.util.concurrent.Executors#newCachedThreadPool()
     */
    @NonNull
    public static ExecutorService newCachedThreadPool(int corePoolSize) {
        return new ThreadPoolExecutor(
                corePoolSize,
                Integer.MAX_VALUE,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                Thread.ofVirtual().factory()
        );
    }
}
