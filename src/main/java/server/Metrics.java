package server;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Metrics {
    private ScheduledExecutorService e = Executors.newSingleThreadScheduledExecutor();
    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    private List<Supplier<String>> suppliers = new ArrayList<>();

    private static final Metrics instance = new Metrics();

    static Metrics getInstance() {
        return instance;
    }

    void addMetric(Supplier<String> supplier){
        suppliers.add(supplier);
    }

    private Metrics() {

        addMetric(() -> "cpu: " + Math.round(operatingSystemMXBean.getSystemLoadAverage())+"%");
        e.scheduleWithFixedDelay(() -> System.out.println((
                suppliers.stream()
                        .map(Supplier::get)
                        .collect(Collectors.joining(", "))))
                ,0,500, TimeUnit.MILLISECONDS);
    }
}
