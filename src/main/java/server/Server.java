package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.*;
import java.nio.file.Files;
import java.security.KeyStore;

public class Server {
    public static SSLEngine sslEngine;
    private final int port;
    public static SslContext sslContext;


    public static void main(String[] args) throws Exception {
        new Server(8000).run();
    }

    public Server(int port) throws Exception {
        this.port = port;
        ssl();
    }

    private void ssl() throws Exception {
        final SelfSignedCertificate cert = new SelfSignedCertificate();
        Server.sslContext = SslContextBuilder.forServer(cert.certificate(), cert.privateKey()).build();
        Server.sslEngine = Server.sslContext.newEngine(ByteBufAllocator.DEFAULT);
        sslEngine.setUseClientMode(false);
    }

    public void run(){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChatServerInitalizer()).localAddress(port);
        try {
            serverBootstrap.bind().sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdown();
            workerGroup.shutdown();
        }
    }
}
