package name.osipov.alexey.server;

import name.osipov.alexey.utils.UnixTime;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

class TimeEncoder extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        UnixTime m = (UnixTime) msg;
        ByteBuf encoded = ctx.alloc().buffer(4);
        encoded.writeInt(m.value());
        ctx.write(encoded, promise); // (1)
    }
}

public class TimeServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(final ChannelHandlerContext ctx) { // (1)
    	ChannelFuture f = ctx.writeAndFlush(new UnixTime());
 
//        final ByteBuf time = ctx.alloc().buffer(4); // (2)
//        time.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));
//
//        final ChannelFuture f = ctx.writeAndFlush(time); // (3)
//        f.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) {
//                assert f == future;
//                ctx.close();
//            }
//        }); // (4)
        
        f.addListener(ChannelFutureListener.CLOSE); // same as above
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}