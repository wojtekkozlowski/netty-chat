package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.text.html.Option;

public class Metrics {
    private ScheduledExecutorService e = Executors.newSingleThreadScheduledExecutor();
    private List<Supplier<Optional<String>>> suppliers = new ArrayList<>();
    int expectedClients;
    double startTime;
    boolean allClientsConnected;

    private static final Metrics instance = new Metrics();

    static Metrics getInstance() {
        return instance;
    }

    void addMetric(Supplier<Optional<String>> supplier) {
        suppliers.add(supplier);
    }

    private Metrics() {
        e.scheduleWithFixedDelay(() -> {
                    String collect = suppliers.stream()
                            .map(Supplier::get)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.joining(", "));
                    System.out.println(collect);
                }
                , 0, 250, TimeUnit.MILLISECONDS);
    }
}
