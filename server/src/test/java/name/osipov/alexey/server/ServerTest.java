package name.osipov.alexey.server;

import org.junit.*;
import static org.junit.Assert.*;

import name.osipov.alexey.httpclient.HttpClient;

/**
 * Unit test for simple App.
 */
public class ServerTest 
{
	private Server s;
	private static final int PORT = 8080; 

	@Before
	public void tearUp()
	{
		s = new Server();
		s.start(PORT, false);
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
		HttpClient c = new HttpClient("127.0.0.1", PORT);
		String response = c.getResponse();
		assertNotNull(response);
		assertNotEquals(response, "");
    }
}
