package server;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.codec.xml.XmlFrameDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

public class TerminalChannelInitalizer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("ssl", new SslHandler(getSslEngine()));
        pipeline.addLast("framer", new XmlFrameDecoder(1048576));
        pipeline.addLast("decoder", new StringDecoder());
        pipeline.addLast("encoder", new StringEncoder());
        pipeline.addLast("handler", new TerminalChannelHandler());
    }

    private SSLEngine getSslEngine() {
        SSLEngine sslEngine = null;
        SelfSignedCertificate cert = Server.selfSignedCertificate;
        try {
            SslContext sslContext = SslContextBuilder.forServer(cert.certificate(), cert.privateKey()).build();
            sslEngine = sslContext.newEngine(ByteBufAllocator.DEFAULT);
            sslEngine.setUseClientMode(false);
        } catch (SSLException e) {
            e.printStackTrace();
        }

        return sslEngine;
    }
}
