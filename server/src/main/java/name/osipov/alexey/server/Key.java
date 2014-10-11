package name.osipov.alexey.server;

import java.util.Random;

public class Key {
	private byte[] value;
	private static Random rnd = new Random(); 

	private Key(byte[] value)
	{
		this.value = value;
	}
	
	public byte[] asBinary()
	{
		return value;
	}

	public static Key Create()
	{
		byte[] val = new byte[64];
		rnd.nextBytes(val);
		return new Key(val);
	}
}
