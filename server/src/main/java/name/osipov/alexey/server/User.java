package name.osipov.alexey.server;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User {
	private int id;
	private Key key;
	private int requests = 0;
	
	public User(int id, Key key)
	{
		this.id = id;
		this.key = key;
	}

	public int getId() {
		return id;
	}
	
	public Key getKey() {
		return key;
	}
	
	public int getRequests() {
		return requests;
	}
	
	public void requestHappen() {
		// TODO: not thread-safe
		requests++;
	}
	
	public byte[] getSaltedHash(final long salt)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(key.asBinary());
			md.update(ByteBuffer.allocate(8).putLong(salt).array());
			return md.digest();
		}
		catch(NoSuchAlgorithmException e)
		{
			throw new RuntimeException("SHA-1 not implemented", e);
		}
	}
}
