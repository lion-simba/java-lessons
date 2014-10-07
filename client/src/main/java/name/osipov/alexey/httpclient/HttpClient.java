package name.osipov.alexey.httpclient;

import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
import io.netty.handler.ssl.SslHandler;

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
	            	if (ssl)
	            	{
	            		// put in SSL handler, trusting to all certificates
	            		SSLContext sslContext = SSLContext.getInstance("TLS");
	                    sslContext.init(null, new TrustManager[]{new X509TrustManager() {
	                    	@Override
	                    	public void checkClientTrusted(X509Certificate[] certs, String s) {}

	                    	@Override
	                  	  	public void checkServerTrusted(X509Certificate[] certs, String s) {}

	                  	  	@Override
	                  	  	public X509Certificate[] getAcceptedIssuers() {
	                  	  		return new X509Certificate[] { null };
	                  	  	}
	                    }}, null);
	                    SSLEngine sslEngine = sslContext.createSSLEngine();
	                    sslEngine.setUseClientMode(true);
	                    ch.pipeline().addLast(new SslHandler(sslEngine));
	            	}
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