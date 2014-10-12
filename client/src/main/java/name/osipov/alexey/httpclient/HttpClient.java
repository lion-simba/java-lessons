package name.osipov.alexey.httpclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpClient implements Callable<String> {
	private String host;
	private int port;
	private boolean ssl;
	private FullHttpRequest request;
	private EventLoopGroup workers;

	private Promise<String> response;
	
	private class HttpClientHandler extends ChannelInboundHandlerAdapter
	{
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			FullHttpMessage ans = (FullHttpMessage)msg;
			try
			{
				ByteArrayOutputStream byte_stream = new ByteArrayOutputStream();
				ans.content().readBytes(byte_stream, ans.content().readableBytes());
				response.setSuccess(byte_stream.toString());
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
	
	private HttpClient(EventLoopGroup workers, String host, int port, boolean ssl, FullHttpRequest request) {
		if (workers == null)
			throw new NullPointerException();
		if (host == null)
			throw new NullPointerException();

		this.workers = workers;
		this.host = host;
		this.port = port;
		this.ssl = ssl;
		this.request = request;
	}
	
	@Override
	public String call() throws Exception {
        Bootstrap b = new Bootstrap();
        b.group(workers);
        b.channel(NioSocketChannel.class);
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

        f.channel().closeFuture().addListener(new ChannelFutureListener() {			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!response.isDone())
					response.setFailure(new ChannelException("Channel is closed"));
			}
		});

        response = workers.next().newPromise();

        f.channel().write(request);
        f.channel().flush();
        
        return response.get();
	}
	
	static public Future<String> makeRequest(EventLoopGroup workers, String host, int port, boolean ssl, FullHttpRequest request) {
		HttpClient http_req = new HttpClient(workers, host, port, ssl, request);
		return workers.submit(http_req);
	}
}
