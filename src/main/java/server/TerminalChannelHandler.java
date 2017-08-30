package server;

import java.util.concurrent.atomic.LongAdder;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class TerminalChannelHandler extends SimpleChannelInboundHandler<String> {

    static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    static final LongAdder msgCounter = new LongAdder();

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        channels.add(incoming);
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("removed!!!");
        super.handlerRemoved(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        msgCounter.increment();

        Channel incoming = ctx.channel();
        String s = msg.replaceFirst("0800", "0810");
        incoming.writeAndFlush(s + "\n");
    }
}
