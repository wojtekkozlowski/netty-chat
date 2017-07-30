package client;

import java.io.IOException;
import java.net.SocketException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    private String host;
    private int port;
    private int connections;
    private int connectionSpread;
    private int echoSpread;
    private final Boolean useSSL;
    private final ExecutorService executorService;
    private int channelTimeout;

    public static void main(String[] args) throws InterruptedException, IOException, ISOException {
        LOGGER.addListener(new SimpleLogListener(System.err));
        System.setProperty("https.protocols", "TLSv1");
        String host;
        int port;
        Boolean useSSL;
        int connections;
        int connectionSpread;
        int echoSpread;
        int connsPerSecond;
        int echoesPerSecond;
        int channelTimeout;
        if (args.length > 0) {
            host = args[0];
            port = Integer.valueOf(args[1]);
            useSSL = Boolean.valueOf(args[2]);
            connections = Integer.valueOf(args[3]);
            connsPerSecond = Integer.valueOf(args[4]);
            echoesPerSecond = Integer.valueOf(args[5]);
            channelTimeout = Integer.valueOf(args[6]);
        } else {
            System.out.println("Usage:");
            System.out.println("<host> <port> <use SSL? true|false> <concurrent_connections> <connections/sec> <echos/sec> <channel_timeout>");
            host = "localhost";
            port = 8000;
            useSSL = true;
            connections = 1000;
            connsPerSecond = 1000;
            echoesPerSecond = 100;
            channelTimeout = 10000;
            System.out.println("using defaults:");
            System.out.println("\n");
        }
        connectionSpread = (int) (connections / (double) connsPerSecond * 1000);
        echoSpread = (int) (connections / (double) echoesPerSecond * 1000);
        System.out.println("\tuse SSL:\t\t\t" + useSSL);
        System.out.println("\tconcurrent_connections:\t\t" + connections);
        System.out.println("\tconnections / sec:\t\t" + connsPerSecond + " /s");
        System.out.println("\ttotal echos / sec:\t\t" + echoesPerSecond + " /s");
        System.out.println("\tchannel timeout in ms:\t\t" + channelTimeout + " ms");
        System.out.println("\n");

        new Client(host, port, useSSL, connections, connectionSpread, echoSpread, channelTimeout);
    }

    private Client(String host, int port, Boolean useSSL, int connections, int connectionSpread, int echoSpread, int channelTimeout) throws ISOException, InterruptedException, IOException {
        this.host = host;
        this.port = port;
        this.useSSL = useSSL;
        this.connections = connections;
        this.connectionSpread = connectionSpread;
        this.echoSpread = echoSpread;
        this.channelTimeout = channelTimeout;
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

    private void startChannel(int i) throws Exception {
        XMLChannel channel = new XMLChannel(host, port, new XMLPackager());
        if (useSSL) {
            channel.setSocketFactory(new SunJSSESocketFactory());
        }
        try {
            channel.setTimeout(channelTimeout);
            channel.setConfiguration(clientConfiguration());
        } catch (ConfigurationException | SocketException e) {
            e.printStackTrace();
        }

        while (true) {
            long start = 0;
            try {
                connectIfNotConnected(channel, i);
                tryToSleep(i);
                start = System.nanoTime();
                channel.send(getIsoMsg());
            } catch (Throwable e) {
                System.out.println(i + ": channel send exception (" + e.getMessage() + ")");
                reconnect(channel);
                return;
            }
            try {
                ISOMsg receive = channel.receive();
                long end = (System.nanoTime() - start);
                if ((end / 1000000) > channelTimeout) {
                    System.out.println("wallclock timeout, timeout is: " + channelTimeout + ", but got response after :" + (end / 1000000));
                    reconnect(channel);
                }
                if (receive == null) {
                    System.out.println("Recevied null response, reconnecting");
                    reconnect(channel);
                }
            } catch (Throwable e) {
                System.out.println(i + ": channel read exception (" + e.getMessage() + ")");
                reconnect(channel);
            }
        }
    }

    private void reconnect(XMLChannel channel) throws Exception {
        channel.disconnect();
        tryToSleep(-1);
        executorService.submit(() -> {
            try {
                startChannel(-1);
            } catch (Exception ex) {
                System.out.println(-1 + ": start channel error (" + ex.getMessage() + ")");
            }
        });

    }

    private void tryToSleep(int i) {
        try {
            Thread.sleep(Double.valueOf(Math.random() * echoSpread).longValue());
        } catch (InterruptedException e) {
            System.out.println(i + " couldn't sleep");
        }
    }

    private void connectIfNotConnected(XMLChannel channel, int i) {
        int conProbs = 0;
        while (!channel.isConnected()) {
            try {
                tryToSleep(i);
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
