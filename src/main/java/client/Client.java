package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLEngine;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client {

    private final String host;
    private final int port;

    public static void main(String[] args) {
        new Client("localhost", 8000).run();
    }

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChatClientInitializer()).remoteAddress(host, port);

        Channel channel = null;
        try {
            channel = bootstrap.connect().sync().channel();
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                channel.writeAndFlush(br.readLine() + "\r\n");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }
}
