package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Server {
    private final int port;
    public static SelfSignedCertificate selfSignedCertificate;

    public static void main(String[] args) throws Exception {
        Metrics.getInstance().addMetric(() -> "channels: "+ TerminalChannelHandler.channels.size());
        Server.selfSignedCertificate = new SelfSignedCertificate();
        Server server = new Server(8000);
        server.run();
    }

    private Server(int port) throws Exception {
        this.port = port;
    }

    private void run(){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new TerminalChannelInitalizer()).localAddress(port);
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
