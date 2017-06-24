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
import server.Server;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {

    private static final Logger LOGGER = new Logger();

    private static final int connections = 2000;
    private static final int connectionSpread = 2;
    private static final int echoSpread = 5;

    public static void main(String[] args) throws InterruptedException, IOException, ISOException {
        LOGGER.addListener(new SimpleLogListener(System.err));
        System.setProperty("https.protocols", "TLSv1");
        new Client();
    }

    Client() throws ISOException, InterruptedException, IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(connections);
        for (int i = 0; i < connections; i++) {
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
        XMLChannel channel = new XMLChannel("localhost", 8000, new XMLPackager());
        if(Server.useSSL) {
            channel.setSocketFactory(new SunJSSESocketFactory());
        }
        try {
            channel.setConfiguration(clientConfiguration());
        } catch (ConfigurationException e) {
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
            Thread.sleep(Double.valueOf(Math.random() * 1000 * echoTimeout).longValue());
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