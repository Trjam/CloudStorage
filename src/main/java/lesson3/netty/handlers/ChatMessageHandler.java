package lesson3.netty.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;

import java.util.concurrent.ConcurrentLinkedDeque;

public class ChatMessageHandler extends SimpleChannelInboundHandler<String> {

    public static final ConcurrentLinkedDeque<SocketChannel> channels = new ConcurrentLinkedDeque<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client connected: " + ctx.channel());
        channels.add((SocketChannel) ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client disconnected: " + ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("Message from client: " + msg);
        msg = msg.replaceAll("lol", "***");
//        ctx.writeAndFlush(msg);
        String finalMsg = msg;
        channels.forEach(c -> c.writeAndFlush(finalMsg));

    }
}
