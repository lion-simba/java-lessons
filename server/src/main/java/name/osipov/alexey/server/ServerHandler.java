/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package name.osipov.alexey.server;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import org.apache.commons.codec.binary.Base64;

public class ServerHandler extends ChannelInboundHandlerAdapter
{
	private Users users;  
	
	public ServerHandler(Users users)
	{
		if (users == null)
			throw new NullPointerException("users must not be null");
		this.users = users;
	}
	
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
                return;
            }

            //String passkey = req.headers().get("passkey");
            
            // in case of large output this buffer will demand memory
            // writing directly to channel maybe more efficient...
            ByteBufOutputStream bufstream = new ByteBufOutputStream(Unpooled.buffer());
            JsonGenerator json = new JsonFactory().createGenerator(bufstream);
            json.writeStartObject();
          
            HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
            
            switch(req.getUri())
            {
            case "/register":
	            {
	            	User u = users.Register();
	            	json.writeNumberField("id", u.getId());
	            	json.writeBinaryField("key", u.getKey().asBinary());
	            	status = HttpResponseStatus.OK;
	            }
            	break;

            case "/statistics":
            	{
            		String hashed_key_base64 = req.headers().get("key");
            		byte[] hashed_key = Base64.decodeBase64(hashed_key_base64);
            		long salt = System.currentTimeMillis()/1000/30;
            		User u = users.getBySaltedHash(hashed_key, salt);
            		if (u != null)
            		{
            			u.requestHappen();
            			json.writeNumberField("id", u.getId());
            			json.writeNumberField("requests", u.getRequests());
            			status = HttpResponseStatus.OK;
            		}
            		else
            			status = HttpResponseStatus.UNAUTHORIZED;
            	}
            	break;
            }

            json.writeEndObject();
            json.close();
            
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, bufstream.buffer());
            response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());

            if (!HttpHeaders.isKeepAlive(req)) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(HttpHeaders.Names.CONNECTION, Values.KEEP_ALIVE);
                ctx.write(response);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
