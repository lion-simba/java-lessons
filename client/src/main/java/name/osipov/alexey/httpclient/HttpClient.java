package name.osipov.alexey.httpclient;

import java.io.ByteArrayOutputStream;

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

public class HttpClient
{
	private ByteArrayOutputStream buf;
	private String host;
	private int port;

	public HttpClient(String host, int port)
	{
		if (host == null)
			throw new NullPointerException();
		this.host = host;
		this.port = port;
	}

	private void connect() throws Exception
	{
		buf = new ByteArrayOutputStream();

		final EventLoopGroup workerGroup = new NioEventLoopGroup();

		try
		{
	        Bootstrap b = new Bootstrap();
	        b.group(workerGroup);
	        b.channel(NioSocketChannel.class);
	        b.option(ChannelOption.SO_KEEPALIVE, true);
	        b.handler(new ChannelInitializer<SocketChannel>() {
	            @Override
	            public void initChannel(SocketChannel ch) throws Exception {
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
        HttpClient c = new HttpClient("127.0.0.1", 8080);
        System.out.println(c.getResponse());
    }
}