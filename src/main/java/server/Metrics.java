package server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Metrics {
    private ScheduledExecutorService e = Executors.newSingleThreadScheduledExecutor();
    private List<Supplier<String>> suppliers = new ArrayList<>();

    private static final Metrics instance = new Metrics();

    static Metrics getInstance() {
        return instance;
    }

    void addMetric(Supplier<String> supplier) {
        suppliers.add(supplier);
    }

    private Metrics() {
        e.scheduleWithFixedDelay(() -> System.out.println((
                        suppliers.stream()
                                .map(Supplier::get)
                                .collect(Collectors.joining(", "))))
                , 0, 250, TimeUnit.MILLISECONDS);
    }
}
