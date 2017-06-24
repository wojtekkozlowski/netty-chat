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
        // SSL
        Boolean useSSL = args.length > 0 ? Boolean.valueOf(args[0]) : true;
        Server server = new Server(8000, useSSL);
        server.run();
    }

    private Server(int port, Boolean useSSL) throws Exception {
        this.useSSL = useSSL;
        System.out.println("\n\nUse SSL: " + this.useSSL + "\n\n");
        this.port = port;
        SelfSignedCertificate cert = new SelfSignedCertificate();
        Server.sslContext = SslContextBuilder.forServer(cert.certificate(), cert.privateKey()).build();
    }

    private void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new TerminalChannelInitalizer(useSSL)).localAddress(port);
        try {
            serverBootstrap.bind().sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
