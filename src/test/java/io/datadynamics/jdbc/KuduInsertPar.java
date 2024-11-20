package io.datadynamics.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Haneul, Kim
 * @version 1.0.0
 * @since 2024-11-20
 */
public class KuduInsertPar {

    public static void main(String[] args) throws InterruptedException {
        int dataCount = 1_000_000;
        int chunkSize = 100;

        int availableProcessors = Runtime.getRuntime().availableProcessors() * 2;
        System.out.println("availableProcessors = " + availableProcessors);
        ExecutorService executor = Executors.newFixedThreadPool(availableProcessors);
        List<Callable<Integer>> callables = new ArrayList<>(availableProcessors);
        for (int i = 0; i < availableProcessors; i++) {
            callables.add(new KuduInsertCallable(i, dataCount, chunkSize));
        }
        executor.invokeAll(callables);
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

}
