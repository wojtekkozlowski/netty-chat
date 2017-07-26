package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.NettyRuntime;
import io.netty.util.internal.SystemPropertyUtil;

public class Server {

    private Boolean useSSL;

    static SslContext sslContext;
    private final int port;
    private static List<Integer> last4 = new ArrayList<>();
    private static Double previousSum = 0d;

    public static void main(String[] args) throws Exception {
        Boolean useSSL;
        int port;

        if (args.length == 2) {
            useSSL = Boolean.valueOf(args[0]);
            port = Integer.valueOf(args[1]);
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
        addMetric();
        new Server(port, useSSL).run();
    }

    private static void addMetric() {

        System.out.println("cpus: " + Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2)));

        System.out.println("connections, rate");
        Metrics.getInstance().addMetric(() -> {
            if (Server.last4.size() < 4) {
                Server.last4.add(TerminalChannelHandler.channels.size());
                return Optional.of("" + TerminalChannelHandler.channels.size());
            } else {
                double currentSum = last4.parallelStream().mapToDouble(d -> d).sum();
                double currentRate = (currentSum - previousSum) / 4;

                String s = "" + TerminalChannelHandler.channels.size() + ", " + currentRate;
                last4.clear();
                previousSum = currentSum;
                return Optional.of(s);
            }
        });
    }

    private Server(int port, Boolean useSSL) throws Exception {
        this.useSSL = useSSL;
        this.port = port;
        SelfSignedCertificate cert = new SelfSignedCertificate();
        Server.sslContext = SslContextBuilder.forServer(cert.certificate(), cert.privateKey()).build();
    }

    private void run() {
        EventLoopGroup acceptorGroup = new NioEventLoopGroup();
        EventLoopGroup clientGroup = new NioEventLoopGroup();
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
