package name.osipov.alexey.server;


public class Users
{
	int next_id = 1;
	public User Register()
	{
		return new User(next_id++, Key.Create());
	}
}
