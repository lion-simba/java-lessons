package name.osipov.alexey.server;

import java.util.Arrays;
import java.util.Collection;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.*;
import org.junit.runners.Parameterized.*;

import static org.junit.Assert.*;

import name.osipov.alexey.httpclient.HttpClient;

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
		String response = c.getResponse();
		assertNotNull(response);
		assertNotEquals(response, "");
    }
	
	@Test
	public void testSslIsSsl() throws Exception
	{
		HttpClient c = new HttpClient("127.0.0.1", PORT, !ssl);
		String response = c.getResponse();
		assertEquals(response, "");
	}
}
