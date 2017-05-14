package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Server {
    private final int port;
    private ChatServerInitalizer childHandler;

    public static void main(String[] args) {
        new Server(8000).run();
    }

    public Server(int port){
        this.port = port;
    }

    public void run(){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        childHandler = new ChatServerInitalizer();
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossGroup,workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(childHandler).localAddress(port);
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
