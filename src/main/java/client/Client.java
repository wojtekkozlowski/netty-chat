package client;

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

import java.io.IOException;
import java.net.SocketException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {

    private static final Logger LOGGER = new Logger();

    private String host;
    private int port;
    private int connections;
    private int connectionSpread;
    private int echoSpread;
    private final Boolean useSSL;

    public static void main(String[] args) throws InterruptedException, IOException, ISOException {
        LOGGER.addListener(new SimpleLogListener(System.err));
        System.setProperty("https.protocols", "TLSv1");
        String host;
        int port;
        Boolean useSSL;
        int connections;
        int connectionSpread;
        int echoSpread;
        if (args.length == 6) {
            host = args[0];
            port = Integer.valueOf(args[1]);
            useSSL = Boolean.valueOf(args[2]);
            connections = Integer.valueOf(args[3]);
            connectionSpread = Integer.valueOf(args[4]);
            echoSpread = Integer.valueOf(args[5]);
        } else {
            host = "localhost";
            port = 8000;
            useSSL = true;
            connections = 1000;
            connectionSpread = 1000;
            echoSpread = 3000;
            System.out.println("Usage:");
            System.out.println("<host> <port> <use SSL? true|false> <concurrent_connections> <connect_spread_in_millis> <echo_spread_in_millis>");
            System.out.println("using defaults:");
            System.out.println("\n");
        }
        System.out.println("\tuse SSL:\t\t" + useSSL);
        System.out.println("\tconcurrent_connections:\t" + connections);
        System.out.println("\tconnect_spread_in_sec:\t" + connectionSpread);
        System.out.println("\techo_spread_in_sec:\t" + echoSpread);
        System.out.println("\tconnections/sec:\t" + ((double) connections / connectionSpread) * 1000);
        System.out.println("\techos/sec:\t\t" + ((double) connections / echoSpread) * 1000);
        System.out.println("\n");

        new Client(host, port, useSSL, connections, connectionSpread, echoSpread);
    }

    Client(String host, int port, Boolean useSSL, int connections, int connectionSpread, int echoSpread) throws ISOException, InterruptedException, IOException {
        this.host = host;
        this.port = port;
        this.useSSL = useSSL;
        this.connections = connections;
        this.connectionSpread = connectionSpread;
        this.echoSpread = echoSpread;
        ExecutorService executorService = Executors.newFixedThreadPool(this.connections);
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
            channel.setTimeout(10000);
            channel.setConfiguration(clientConfiguration());
        } catch (ConfigurationException | SocketException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                connectIfNotConnected(channel, i);
                tryToSleep(i, echoSpread);
                channel.send(getIsoMsg());
            } catch (Exception e) {
                System.out.println(i + ": channel send error (" + e.getMessage() + ")");
            }
            try {
                channel.receive();
            } catch (IOException | ISOException e) {
                System.out.println(i + ": channel read error (" + e.getMessage() + ")");
            }
        }
    }

    private void tryToSleep(int i, int echoTimeout) {
        try {
            Thread.sleep(Double.valueOf(Math.random() * echoTimeout).longValue());
        } catch (InterruptedException e) {
            System.out.println(i + " couldn't sleep");
        }
    }

    private void connectIfNotConnected(XMLChannel channel, int i) {
        int conProbs = 0;
        while (!channel.isConnected()) {
            try {
                tryToSleep(i, connectionSpread);
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
