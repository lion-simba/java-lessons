package name.osipov.alexey.server;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import name.osipov.alexey.httpclient.HttpClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(Parameterized.class)
public class ServerTest 
{
	private static final int PORT = 8080;
	
	@Parameter
	public boolean ssl;

	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                 { true }, { false }  
           });
    }

    private Server s;
    private EventLoopGroup workers;
    
	@Before
	public void tearUp()
	{
		s = new Server();
		s.start(PORT, ssl);
		
		workers = new NioEventLoopGroup();
	}

	@After
	public void tearDown()
	{
		workers.shutdownGracefully();

		s.stop();
		s = null;
	}

	@Test(timeout=5000)
    public void testConnect() throws Exception
    {
		String response = HttpClient.makeRequest(workers, "127.0.0.1", PORT, ssl,
				new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")).get();
		assertNotNull(response);
		assertNotEquals(response, "");
    }
	
	@Test
	public void testRegister() throws Exception
	{
		ObjectMapper m = new ObjectMapper();
		String response;
		JsonNode ans;
		
		response = HttpClient.makeRequest(workers, "127.0.0.1", PORT, ssl,
			new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/register")).get();
		ans = m.readTree(response);
		int id1 = ans.get("id").asInt();
		String key1 = ans.get("key").asText();
		assertNotNull(key1);
		assertNotEquals(key1, "");

		response = HttpClient.makeRequest(workers, "127.0.0.1", PORT, ssl,
				new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/register")).get();
		ans = m.readTree(response);
		int id2 = ans.get("id").asInt();
		String key2 = ans.get("key").asText();
		assertNotNull(key2);
		assertNotEquals(key2, "");
		
		assertNotEquals(id1, id2);
		assertNotEquals(key1, key2);
	}

	@Test(expected=ExecutionException.class)
	public void testSslIsSsl() throws Exception
	{
		HttpClient.makeRequest(workers, "127.0.0.1", PORT, !ssl,
				new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")).sync();
	}
}
