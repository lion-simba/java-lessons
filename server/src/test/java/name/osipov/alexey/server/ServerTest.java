package name.osipov.alexey.server;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import io.netty.channel.ChannelException;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Arrays;
import java.util.Collection;

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
    
	@Before
	public void tearUp()
	{
		s = new Server();
		s.start(PORT, ssl);
	}

	@After
	public void tearDown()
	{
		s.stop();
		s = null;
	}

	@Test(timeout=5000)
    public void testConnect() throws Exception
    {
		HttpClient c = new HttpClient("127.0.0.1", PORT, ssl);
		c.doRequest(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"));
		String response = c.getResponse();
		assertNotNull(response);
		assertNotEquals(response, "");
    }
	
	@Test
	public void testRegister() throws Exception
	{
		HttpClient c = new HttpClient("127.0.0.1", PORT, ssl);
		ObjectMapper m = new ObjectMapper();
		String response;
		JsonNode ans;
		
		c.doRequest(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/register"));
		response = c.getResponse();
		ans = m.readTree(response);
		int id1 = ans.get("id").asInt();
		String key1 = ans.get("key").asText();
		assertNotNull(key1);
		assertNotEquals(key1, "");

		c.doRequest(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/register"));
		response = c.getResponse();
		ans = m.readTree(response);
		int id2 = ans.get("id").asInt();
		String key2 = ans.get("key").asText();
		assertNotNull(key2);
		assertNotEquals(key2, "");
		
		assertNotEquals(id1, id2);
		assertNotEquals(key1, key2);
	}
	
	@Test(expected=ChannelException.class)
	public void testSslIsSsl() throws Exception
	{
		HttpClient c = new HttpClient("127.0.0.1", PORT, !ssl);
		c.doRequest(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"));
		c.getResponse();
	}
}
