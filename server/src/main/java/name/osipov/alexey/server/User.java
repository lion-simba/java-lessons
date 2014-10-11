package name.osipov.alexey.server;

public class User {
	private int id;
	private Key key;
	
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
}
