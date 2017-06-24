package client;

import org.jpos.core.Configuration;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.SunJSSESocketFactory;
import org.jpos.iso.channel.XMLChannel;
import org.jpos.iso.packager.XMLPackager;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogListener;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {

    private static final Logger LOGGER = new Logger();
    private static final int timeframe = 2;
    private static final int connections = 1000;


    public static void main(String[] args) throws InterruptedException, IOException, ISOException {
        LOGGER.addListener(new SimpleLogListener(System.err));
        System.setProperty("https.protocols", "TLSv1");
        new Client();
    }

    Client() throws ISOException, InterruptedException, IOException {
        ExecutorService es = Executors.newFixedThreadPool(connections);
        for (int i = 0; i < connections; i++) {
            es.submit(() -> {
                try {
                    startChannel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    private void startChannel() throws ISOException, IOException {
        XMLChannel channel = new XMLChannel(new XMLPackager());
        channel.setHost("localhost", 8000);
        channel.setSocketFactory(new SunJSSESocketFactory());
        channel.setConfiguration(clientConfiguration());

        connectIfNotConnected(channel);

        while (true) {
            try {
                tryToSleep();
                channel.send(getIsoMsg());
                channel.receive();
            } catch (Exception e) {
                e.printStackTrace();
                connectIfNotConnected(channel);
            }
        }
    }

    private void tryToSleep() {
        try {
            Thread.sleep(Double.valueOf(Math.random() * 1000 * timeframe).longValue());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void connectIfNotConnected(XMLChannel channel) {
        int conProbs = 0;
        if (!channel.isConnected()) {
            while (!channel.isConnected()) {
                try {
                    tryToSleep();
                    channel.connect();
                } catch (IOException e) {
                    if (conProbs == 0) {
                        System.out.println("couldn't connect, retrying");
                        conProbs += 1;
                    } else {
                        System.out.println("couldn't connect, retrying: " + conProbs++);
                    }
                }
            }
            if (conProbs > 0) {
                System.out.println("finally connected after: " + conProbs + " attempts");
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
        props.put("timeout", "2000");
        props.put("connect-timeout", "2000");
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