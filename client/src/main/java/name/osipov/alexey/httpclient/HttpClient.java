package name.osipov.alexey.httpclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslHandler;

import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpClient
{
	private String host;
	private int port;
	private boolean ssl;

	private EventLoopGroup workerGroup;
	
	private ByteArrayOutputStream response;
	private ChannelPromise responseFuture;

	private class HttpClientHandler extends ChannelInboundHandlerAdapter
	{
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			FullHttpMessage ans = (FullHttpMessage)msg;
			try
			{
				response = new ByteArrayOutputStream();
				ans.content().readBytes(response, ans.content().readableBytes());
				responseFuture.setSuccess();
				ctx.close();
			}
			finally
			{
				ans.release();
			}
		}

	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	    	cause.printStackTrace();
	        ctx.close();
	    }
	}

	public HttpClient(String host, int port, boolean ssl)
	{
		if (host == null)
			throw new NullPointerException();
		this.host = host;
		this.port = port;
		this.ssl = ssl;
	}
	
	public void doRequest(FullHttpRequest request) throws InterruptedException
	{
		if (request == null)
			throw new NullPointerException("request must not be null");

		response = null;
		responseFuture = null;

		workerGroup = new NioEventLoopGroup();

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
	                		new HttpClientHandler());
	            }
	        });

	        // Start the client.
	        ChannelFuture f = b.connect(host, port).sync();

	        responseFuture = f.channel().newPromise();
	        
	        f.channel().closeFuture().addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!responseFuture.isDone())
						responseFuture.setFailure(new ChannelException("Channel is closed"));
					workerGroup.shutdownGracefully();
				}
			});
	        
	        request.headers().add("passkey", "bravo");
	        f.channel().write(request);
	        f.channel().flush();
		}
		catch(Exception e)
		{
			workerGroup.shutdownGracefully();
			throw e;
		}
	}
	
	public String getResponse() throws InterruptedException
	{
		if (responseFuture == null)
			throw new IllegalStateException("No request has been done");
		responseFuture.sync();
		return response.toString();
	}
	
    public static void main(String[] args) throws Exception
    {
        HttpClient c = new HttpClient("127.0.0.1", 8080, false);
        System.out.println(c.getResponse());
    }
}