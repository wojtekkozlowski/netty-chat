package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;

/**
 * Created by wojtek on 14/05/2017.
 */
public class ChatClientHandler extends ChannelInboundMessageHandlerAdapter<String>{

    @Override
    public void messageReceived(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        System.out.println(s);
    }
}
