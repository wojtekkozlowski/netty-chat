package server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;

/**
 * Created by wojtek on 14/05/2017.
 */
public class ChatServerHandler extends ChannelInboundMessageHandlerAdapter<String> {

    @Override
    public void afterAdd(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        for (Channel channel : channels){
            channel.write("[SERVER] - " +incoming.remoteAddress() + " has joined");
        }
        channels.add(incoming);
    }

    @Override
    public void afterRemove(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        for (Channel channel : channels){
            channel.write("[SERVER] - " +incoming.remoteAddress() + " has left");
        }
        channels.remove(incoming);
    }

    public static final ChannelGroup channels = new DefaultChannelGroup();
    @Override
    public void messageReceived(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        Channel incoming = channelHandlerContext.channel();
        for (Channel channel : channels) {
            if (channel != incoming){
                channel.write("[" + incoming.remoteAddress() + "]" + s + "\r\n");
            }
        }
    }
}
