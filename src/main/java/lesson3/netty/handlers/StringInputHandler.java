package lesson3.netty.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class StringInputHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = String.valueOf(msg);
        System.out.println("message: " + message.replace("\n", ""));
        ctx.write(message);
    }
}
