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
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.NettyRuntime;
import io.netty.util.internal.SystemPropertyUtil;

public class Server {

    private Boolean useSSL;

    static SslContext sslContext;
    private final int port;
    private static List<Integer> last4Connections = new ArrayList<>();
    private static Double previousConnectionsSum = 0d;

    public static void main(String[] args) throws Exception {
        Boolean useSSL;
        int port;
        int expectedClients;

        if (args.length == 3) {
            useSSL = Boolean.valueOf(args[0]);
            port = Integer.valueOf(args[1]);
            expectedClients = Integer.valueOf(args[2]);
        } else {
            System.out.println("Usage:");
            System.out.println("  <use SSL? true|false> <port> <expected clients>");
            System.out.println("  using defaults:");
            port = 8000;
            expectedClients = -1;
            useSSL = true;
        }
        System.out.println("\tUse SSL: " + useSSL);
        System.out.println("\tport: " + port);
        addMetric(expectedClients);
        new Server(port, useSSL).startBootstrap();
    }

    private Server(int port, Boolean useSSL) throws Exception {
        this.useSSL = useSSL;
        this.port = port;
        SelfSignedCertificate cert = new SelfSignedCertificate();
        Server.sslContext = SslContextBuilder.forServer(cert.certificate(), cert.privateKey()).sslProvider(SslProvider.OPENSSL).build();
    }

    private void startBootstrap() {
        EventLoopGroup acceptorGroup = new NioEventLoopGroup(1);
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

    private static void addMetric(int expectedClients) {
        System.out.println("cpus: " + Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2)));
        System.out.println("connections, new connections/s, messages/s");
        Metrics metrics = Metrics.getInstance();
        metrics.expectedClients = expectedClients;
        metrics.addMetric(() -> {
            int connections = TerminalChannelHandler.channels.size();
            if (metrics.expectedClients != -1) {
                if (!metrics.allClientsConnected) {
                    if (connections == metrics.expectedClients) {
                        double duration = System.currentTimeMillis() - metrics.startTime;
                        double rate = connections / (duration / 1000);
                        rate = Math.round(rate * 10) / 10;
                        System.out.println(">> Connected all " + connections + " clients, rate: " + rate + "/s");
                        metrics.allClientsConnected = true;
                    } else if (connections > 0 && metrics.startTime == 0) {
                        metrics.startTime = System.currentTimeMillis();
                        System.out.println(">> Starting timer");
                    } else if (connections == 0 && metrics.allClientsConnected) {
                        metrics.allClientsConnected = false;
                    }
                }
            }

            if (Server.last4Connections.size() < 3) {
                Server.last4Connections.add(connections);
                return Optional.of("" + connections);
            } else {
                double currentconnectionsSum = last4Connections.parallelStream().mapToDouble(d -> d).sum();
                double currentconnectionsRate = (currentconnectionsSum - previousConnectionsSum) / 3;
                currentconnectionsRate = Math.round(currentconnectionsRate * 10) / 10;

                String s = "" + connections + ", " + currentconnectionsRate + " conn/sec";
                last4Connections.clear();
                previousConnectionsSum = currentconnectionsSum;
                return Optional.of(s);
            }
        });
    }
}
