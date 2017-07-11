package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Server {

    public Boolean useSSL;

    static SslContext sslContext;
    private final int port;

    public static void main(String[] args) throws Exception {
        Metrics.getInstance().addMetric(() -> "channels: " + TerminalChannelHandler.channels.size());
        Boolean useSSL;
        int port;
        if (args.length == 2){
            useSSL =  Boolean.valueOf(args[0]);
            port =  Integer.valueOf(args[1]);
        } else {
            System.out.println("Usage:");
            System.out.println("<use SSL? true|false> <port>");
            System.out.println("using defaults:");
            port = 8000;
            useSSL = true;
        }
        System.out.println("\n");
        System.out.println("\tUse SSL: " + useSSL);
        System.out.println("\tport: " + port);
        System.out.println("\n");
        Server server = new Server(port, useSSL);
        server.run();
    }

    private Server(int port, Boolean useSSL) throws Exception {
        this.useSSL = useSSL;
        this.port = port;
        SelfSignedCertificate cert = new SelfSignedCertificate();
        Server.sslContext = SslContextBuilder.forServer(cert.certificate(), cert.privateKey()).build();
    }

    private void run() {
        EventLoopGroup acceptorGroup = new NioEventLoopGroup(8);
        EventLoopGroup clientGroup = new NioEventLoopGroup(8);
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(acceptorGroup, clientGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new TerminalChannelInitalizer(useSSL)).localAddress(port);
        try {
            serverBootstrap.bind().sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            acceptorGroup.shutdownGracefully();
            clientGroup.shutdownGracefully();
        }
    }
}
