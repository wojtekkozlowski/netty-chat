package server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOStringFieldPackager;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.iso.packager.XMLPackager;

import java.util.List;

public class ChatServerHandler extends SimpleChannelInboundHandler<String> {

    public static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        System.out.println("[SERVER]: " + incoming.remoteAddress() + " connected\r\n");
        channels.add(incoming);
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        System.out.println("[SERVER]: " + incoming.remoteAddress() + " left\r\n");
        super.handlerRemoved(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        Channel incoming = ctx.channel();
        String s = msg.replaceFirst("0800", "0810");
        System.out.println("[SERVER] " + incoming.remoteAddress() + " replying: " + s + "\r\n");
        incoming.writeAndFlush(s+"\n");
    }
}
