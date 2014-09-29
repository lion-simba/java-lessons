package name.osipov.alexey.server;

import java.io.IOException;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

/**
 * Handles a server-side channel.
 */
public class DiscardServerHandler extends ChannelInboundHandlerAdapter { // (1)

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws IOException { // (2)
        ByteBuf in = (ByteBuf) msg;
       	String text = in.toString(CharsetUtil.US_ASCII);
        System.out.print(text);
        
    	ctx.write(msg);
    	ctx.flush();
    	
    	if (text.trim().equalsIgnoreCase("bye")) {
        	final ByteBuf out = ctx.alloc().buffer(100);
        	try(final ByteBufOutputStream bufstream = new ByteBufOutputStream(out)) {
        		bufstream.writeBytes("Bye bye!" + System.lineSeparator());
        	}
        	ctx.writeAndFlush(out).addListener(new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture future) {
                  ctx.close();
              }
        	});
        }

    	if (text.trim().equalsIgnoreCase("quit")) {
        	final ByteBuf out = ctx.alloc().buffer(100);
        	try(final ByteBufOutputStream bufstream = new ByteBufOutputStream(out)) {
        		bufstream.writeBytes("Shutting down..." + System.lineSeparator());
        	}
        	ctx.writeAndFlush(out).addListener(new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture future) {
                  ctx.channel().parent().close();
              }
        	});
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws IOException { // (1)
//        final ByteBuf time = ctx.alloc().buffer(4); // (2)
//        time.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));

    	final ByteBuf buf = ctx.alloc().buffer(100);
    	try (final ByteBufOutputStream bufstream = new ByteBufOutputStream(buf)) {
    		bufstream.writeBytes("Hello, stranger!" + System.lineSeparator());
    	}
    	ctx.writeAndFlush(buf);
//        final ChannelFuture f = ctx.writeAndFlush(time); // (3)
//        f.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) {
//                assert f == future;
//                ctx.close();
//            }
//        }); // (4)
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}