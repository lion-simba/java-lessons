package name.osipov.alexey.server;

import java.util.Arrays;
import java.util.LinkedList;

// Хранить набор предпосчитанных хэшей для ключей.
// При запросе досчитывать хэш с таймстампом.
// Надо просто считать при первом запросе.
// Можно ещё предпросчитывать первые пару байт таймстемпа.
// Надо иметь мап хэш->юзер. Завести тред, который будет немного наперед считать хэши для всех пользователей.
// На каждого пользователя надо как минимум 3 хэша хранить: текущий период, предыдущий и следующий: чтобы не наступить на границу периода.
// Можно отдельный тред не заводить, а просто кэшировать посчитанные хэши в общедоступном мапе. Мап должен инвалидироваться после окончания периода.

public class Users
{
	private LinkedList<User> users = new LinkedList<User>();
	
	private int next_id = 1;
	public User Register()
	{
		User user = new User(next_id++, Key.Create());
		users.add(user);
		return user;
	}
	
	/**
	 * Return user by hash of it's private key and salt
	 * @param hash
	 * @return user if found, null if not found
	 */
	public User getBySaltedHash(byte[] salted_hash, long salt)
	{
		for(User u : users)
		{
			if (Arrays.equals(salted_hash, u.getSaltedHash(salt)))
				return u;
		}
		return null;
	}
	
	/**
	 * Return user by hash of it's private key and salt with specified tolerance,
	 * i.e. checking is done for each salt in range [salt-tolerance;salt+tolerance].
	 * @param salted_hash
	 * @param salt
	 * @param tolerance
	 * @return user if found, null if not found
	 */
	public User getBySaltedHash(byte[] salted_hash, long salt, int tolerance)
	{
		// most frequent case
		User u = getBySaltedHash(salted_hash, salt);
		if (u != null)
			return u;
		
		// less frequent cases
		for(long s = salt-1; s >= salt-tolerance; s--)
		{
			u = getBySaltedHash(salted_hash, s);
			if (u != null)
				return u;
		}
		// least frequent cases
		for(long s = salt+1; s <= salt+tolerance; s++)
		{
			u = getBySaltedHash(salted_hash, s);
			if (u != null)
				return u;
		}

		return null;
	}
}
