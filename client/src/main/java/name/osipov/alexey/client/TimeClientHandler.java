package name.osipov.alexey.client;

import io.netty.buffer.*;
import io.netty.channel.*;

import java.util.Date;

public class TimeClientHandler extends ChannelInboundHandlerAdapter
{
//	private ByteBuf buf;
//
//	@Override
//	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
//		buf = ctx.alloc().buffer(4);
//	}
//	
//	@Override
//	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
//		buf.release();
//		buf = null;
//	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
//		ByteBuf m = (ByteBuf) msg; // (1)
//		buf.writeBytes(m);
//		m.release();
//
//		if (buf.isReadable(4))
//		{
//			long currentTimeMillis = (buf.readUnsignedInt() - 2208988800L) * 1000L;
//			System.out.println(new Date(currentTimeMillis));
//			ctx.close();
//		}
		ByteBuf m = (ByteBuf)msg;
		try
		{
			long currentTimeMillis = (m.readUnsignedInt() - 2208988800L) * 1000L;
			System.out.println(new Date(currentTimeMillis));
			ctx.close();
		}
		finally
		{
			m.release();
		}
	}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}