package name.osipov.alexey.server;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import name.osipov.alexey.http.HttpHelloWorldServerHandler;
import io.netty.bootstrap.ServerBootstrap;

import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Discards any incoming data.
 */
public class Server
{
    private Channel listener;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private SslContext getSslCtx()
    {
		try {
			SelfSignedCertificate ssc = new SelfSignedCertificate();			
			return SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
		} catch (CertificateException | SSLException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    public void start(int port, boolean ssl)
    {
    	if (listener != null)
    		return;
  
    	// Configure SSL.
        final SslContext sslCtx;
        if (ssl)
        	sslCtx = getSslCtx();
        else
        	sslCtx = null;
 
    	bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try
        {
	        ServerBootstrap b = new ServerBootstrap();
	        b.group(bossGroup, workerGroup)
	         .channel(NioServerSocketChannel.class)
	         .handler(new LoggingHandler(LogLevel.INFO))
	         .childHandler(new ChannelInitializer<SocketChannel>() {
	            	@Override
	        	    public void initChannel(SocketChannel ch) {
	        	        ChannelPipeline p = ch.pipeline();
	        	        if (sslCtx != null) {
	        	            p.addLast(sslCtx.newHandler(ch.alloc()));
	        	        }
	        	        p.addLast(new HttpServerCodec());
	        	        p.addLast(new HttpHelloWorldServerHandler());
	        	    }
	             })
	         .option(ChannelOption.SO_BACKLOG, 128);

	        // Bind and start to accept incoming connections.
	        ChannelFuture f = b.bind(port).sync();
	        listener = f.channel();
        }
        catch(Exception e)
        {
        	workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    
    public void stop()
    {
    	if (listener == null)
    		return;
 
    	try {
			listener.close().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

    	workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        Server s = new Server();
        s.start(port, false);
        //Thread.sleep(10000);
        //s.stop();
    }
}