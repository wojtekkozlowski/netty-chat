package server;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.codec.xml.XmlFrameDecoder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

public class ChatServerInitalizer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        SSLEngine sslEngine = Server.sslContext.newEngine(ByteBufAllocator.DEFAULT);
        sslEngine.setUseClientMode(false);
        pipeline.addLast("ssl", new SslHandler(sslEngine));
        pipeline.addLast("framer", new XmlFrameDecoder(1048576));
        pipeline.addLast("decoder", new StringDecoder());
        pipeline.addLast("encoder", new StringEncoder());
        pipeline.addLast("handler", new ChatServerHandler());
    }
}
