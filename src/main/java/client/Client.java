package client;

import java.io.IOException;
import java.net.SocketException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.SunJSSESocketFactory;
import org.jpos.iso.channel.XMLChannel;
import org.jpos.iso.packager.XMLPackager;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogListener;

public class Client {

    private static final Logger LOGGER = new Logger();
    public static final int TIMEOUT = 30000;

    private String host;
    private int port;
    private int connections;
    private final Boolean useSSL;
    private final ExecutorService executorService;
    private static LongAdder responses = new LongAdder();
    private static ScheduledExecutorService metrics = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws InterruptedException, IOException, ISOException {
        LOGGER.addListener(new SimpleLogListener(System.err));
        System.setProperty("https.protocols", "TLSv1");
        String host;
        int port;
        Boolean useSSL;
        int connections;
        if (args.length > 0) {
            host = args[0];
            port = Integer.valueOf(args[1]);
            useSSL = Boolean.valueOf(args[2]);
            connections = Integer.valueOf(args[3]);
        } else {
            System.out.println("Usage:");
            System.out.println("<host> <port> <use SSL? true|false> <concurrent_connections>");
            host = "localhost";
            port = 8000;
            useSSL = true;
            connections = 1000;
            System.out.println("using defaults:");
            System.out.println("\n");
        }
        System.out.println("\tuse SSL:\t\t\t" + useSSL);
        System.out.println("\tconcurrent_connections: \t\t" + connections);
        System.out.println("\n");

        metrics.scheduleAtFixedRate(() -> System.out.println("responses: " + responses.sum()), 0, 1, TimeUnit.SECONDS);

        new Client(host, port, useSSL, connections);
    }

    private Client(String host, int port, Boolean useSSL, int connections) throws ISOException, InterruptedException, IOException {
        this.host = host;
        this.port = port;
        this.useSSL = useSSL;
        this.connections = connections;
        executorService = Executors.newFixedThreadPool(this.connections);
        for (int i = 0; i < this.connections; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    startChannel(finalI);
                } catch (Exception e) {
                    System.out.println(finalI + ": start channel error (" + e.getMessage() + ")");
                }
            });
        }
        executorService.awaitTermination(10000, TimeUnit.DAYS);
    }

    private void startChannel(int i) throws ISOException {
        XMLChannel channel = new XMLChannel(host, port, new XMLPackager());
        if (useSSL) {
            channel.setSocketFactory(new SunJSSESocketFactory());
        }
        try {
            channel.setTimeout(TIMEOUT);
            channel.setConfiguration(clientConfiguration());
        } catch (ConfigurationException | SocketException e) {
            System.out.println("a");
            e.printStackTrace();
        }

        long start = 0;
        while (true) {
            boolean couldSend = false;
            while (!couldSend) {
                try {
                    connectIfNotConnected(channel, i);
                    start = System.nanoTime();
                    channel.send(getIsoMsg());
                    couldSend = true;
                } catch (Throwable e) {
                    System.out.println(i + ": channel send exception (" + e.getMessage() + ")");
                    reconnect(channel);
                }
            }
            try {
                ISOMsg receive = channel.receive();

                long end = (System.nanoTime() - start);
                if ((end / 1000000) > TIMEOUT) {
                    System.out.println("wallclock timeout, timeout is: " + TIMEOUT + ", but got response after :" + (end / 1000000));
                    reconnect(channel);
                }
                if (receive == null) {
                    System.out.println("Recevied null response, reconnecting");
                    reconnect(channel);
                } else {
                    responses.increment();
                }
            } catch (Throwable e) {
                System.out.println(i + ": channel read exception (" + e.getMessage() + ")");
                reconnect(channel);
            } finally {
                tryToSleep(50);
            }
        }
    }

    private void tryToSleep(int seconds) {
        try {
            Thread.sleep(1000 * seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void reconnect(XMLChannel channel) {
        System.out.println("reconnect!");
        try {
            channel.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        executorService.submit(() -> {
            try {
                startChannel(-1);
            } catch (Exception ex) {
                System.out.println(-1 + ": start channel error (" + ex.getMessage() + ")");
            }
        });

    }

    private void connectIfNotConnected(XMLChannel channel, int i) {
        int conProbs = 0;
        while (!channel.isConnected()) {
            try {
                channel.connect();
            } catch (IOException e) {
                if (conProbs == 0) {
                    System.out.println(i + ": couldn't connect, retrying (" + e.getMessage() + ")");
                    conProbs += 1;
                } else {
                    System.out.println(i + ": couldn't connect, retrying: " + conProbs++ + ", (" + e.getMessage() + ")");
                }
            }
        }
    }

    private Configuration clientConfiguration() {
        Properties props = new Properties();
        props.put("keystore", "src/main/resources/keystore.jks");
        props.put("serverauth", "false");
        props.put("storepassword", "qwerty");
        props.put("keypassword", "qwerty");
        props.put("addEnabledCipherSuite", "SSL_RSA_WITH_3DES_EDE_CBC_SHA");
        props.put("timeout", "5000");
        props.put("connect-timeout", "5000");
        return new SimpleConfiguration(props);
    }

    private ISOMsg getIsoMsg() {
        ISOMsg req = new ISOMsg();
        try {
            req.setMTI("0800");
        } catch (ISOException e) {
            e.printStackTrace();
        }
        req.set(3, "000003");
        req.set(41, "00000041");
        req.set(70, "301");
        return req;
    }
}
