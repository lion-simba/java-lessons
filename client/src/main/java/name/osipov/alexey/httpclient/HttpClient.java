package name.osipov.alexey.httpclient;

import java.io.ByteArrayOutputStream;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;

public class HttpClient
{
	private ByteArrayOutputStream buf;
	private String host;
	private int port;
	private boolean ssl;

	public HttpClient(String host, int port, boolean ssl)
	{
		if (host == null)
			throw new NullPointerException();
		this.host = host;
		this.port = port;
		this.ssl = ssl;
	}

	private SslContext getSslCtx()
	{
		try {
			return SslContext.newClientContext();
		} catch (SSLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void connect() throws Exception
	{
		buf = new ByteArrayOutputStream();

		final EventLoopGroup workerGroup = new NioEventLoopGroup();
		final SslContext sslCtx;
		if (ssl)
			sslCtx = getSslCtx();
		else
			sslCtx = null;

		try
		{
	        Bootstrap b = new Bootstrap();
	        b.group(workerGroup);
	        b.channel(NioSocketChannel.class);
	        b.option(ChannelOption.SO_KEEPALIVE, true);
	        b.handler(new ChannelInitializer<SocketChannel>() {
	            @Override
	            public void initChannel(SocketChannel ch) throws Exception {
	            	if (sslCtx != null)
	            		ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
	                ch.pipeline().addLast(
	                		new HttpClientCodec(),
	                		new HttpObjectAggregator(1024*1024),
	                		new HttpClientHandler(buf));
	            }
	        });

	        // Start the client.
	        ChannelFuture f = b.connect(host, port).sync();

	        DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://" + host);
	        req.headers().add("passkey", "bravo");
	        f.channel().write(req);
	        f.channel().flush();

	        f.channel().closeFuture().sync();
		}
		finally
		{
			workerGroup.shutdownGracefully();
		}
	}
	
	public String getResponse() throws Exception
	{
		if (buf == null)
			connect();
		return buf.toString();
	}
	
    public static void main(String[] args) throws Exception
    {
        HttpClient c = new HttpClient("127.0.0.1", 8080, false);
        System.out.println(c.getResponse());
    }
}