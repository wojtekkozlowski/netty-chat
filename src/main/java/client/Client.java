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

public class Client {

    private static final Logger LOGGER = new Logger();

    public static void main(String[] args) {
        LOGGER.addListener(new SimpleLogListener(System.out));
        new Client();
    }

    Client() {
        try {
            XMLChannel channel = new XMLChannel(new XMLPackager());
            channel.setHost("localhost",8000);
            channel.setSocketFactory(new SunJSSESocketFactory());
            channel.setConfiguration(clientConfiguration());
            channel.connect();
            channel.send(getIsoMsg());
            channel.receive();
            channel.disconnect();
        } catch (IOException | ISOException e) {
            e.printStackTrace();
        }
    }

    private Configuration clientConfiguration() {
        Properties props = new Properties();
        props.put("keystore", "src/main/resources/keystore.jks");
        props.put("serverauth", "false");
        props.put("storepassword", "qwerty");
        props.put("keypassword", "qwerty");
        props.put("addEnabledCipherSuite", "SSL_RSA_WITH_3DES_EDE_CBC_SHA");
        props.put("timeout", "1000");
        props.put("connect-timeout", "1000");
        return new SimpleConfiguration(props);
    }

    private ISOMsg getIsoMsg() throws ISOException {
        ISOMsg req = new ISOMsg();
        req.setMTI("0800");
        req.set (3, "000000");
        req.set (41, "00000001");
        req.set (70, "301");
        return req;
    }
}