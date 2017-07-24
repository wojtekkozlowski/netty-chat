package server;

import javax.net.ssl.SSLEngine;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.codec.xml.XmlFrameDecoder;
import io.netty.handler.ssl.SslHandler;

public class TerminalChannelInitalizer extends ChannelInitializer<SocketChannel> {

    private final Boolean useSSL;

    TerminalChannelInitalizer(Boolean useSSL) {
        super();
        this.useSSL = useSSL;
    }

    @Override
    public void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        if (useSSL) {
            pipeline.addLast("ssl", new SslHandler(getSslEngine()));
        }
        pipeline.addLast("framer", new XmlFrameDecoder(1048576));
        pipeline.addLast("decoder", new StringDecoder());
        pipeline.addLast("encoder", new StringEncoder());
        pipeline.addLast("handler", new TerminalChannelHandler());
    }

    private SSLEngine getSslEngine() {
        SSLEngine sslEngine = Server.sslContext.newEngine(ByteBufAllocator.DEFAULT);
        sslEngine.setUseClientMode(false);
        return sslEngine;
    }
}
