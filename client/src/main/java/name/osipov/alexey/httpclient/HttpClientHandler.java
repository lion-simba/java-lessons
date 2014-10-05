package name.osipov.alexey.httpclient;

import java.io.OutputStream;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpMessage;

public class HttpClientHandler extends ChannelInboundHandlerAdapter {
	private OutputStream sink;

	public HttpClientHandler(OutputStream sink)
	{
		this.sink = sink;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		FullHttpMessage ans = (FullHttpMessage)msg;
		try
		{
			ans.content().readBytes(sink, ans.content().readableBytes());
			ctx.close();
		}
		finally
		{
			ans.release();
		}
	}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        ctx.fireExceptionCaught(cause);
    }
}
