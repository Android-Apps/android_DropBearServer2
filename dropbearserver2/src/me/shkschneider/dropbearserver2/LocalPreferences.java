package me.shkschneider.dropbearserver2;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import me.shkschneider.dropbearserver2.util.L;
import me.shkschneider.dropbearserver2.util.RootUtils;
import me.shkschneider.dropbearserver2.util.ServerUtils;

public class LocalPreferences {

	public static final String PREF_ALLOW_PASSWORD = "allow_password";
	public static final Boolean PREF_ALLOW_PASSWORD_DEFAULT = true;
	public static final String PREF_START_BOOT = "Start_boot";
	public static final Boolean PREF_START_BOOT_DEFAULT = true;
	public static final String PREF_PASSWORD = "password";
	public static final String PREF_PASSWORD_DEFAULT = "42";
	public static final String PREF_PORT = "port";
	public static final String PREF_PORT_DEFAULT = "2222";

	public static final int PORT_MIN = 1;
	public static final int PORT_MAX = 65535;

	public static Boolean getBoolean(Context context, String key, Boolean defaultValue) {
		if (context != null) {
			try {
				return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
			}
			catch (ClassCastException e) {
				L.e("ClassCastException: " + e.getMessage());
			}
		}
		else {
			L.w("Context is null");
		}
		return false;
	}

	public static Boolean putBoolean(Context context, String key, Boolean value) {
		if (context != null) {
			L.d(key + " = " + value);
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putBoolean(key, value);
			return editor.commit();
		}
		else {
			L.w("Context is null");
		}
		return false;
	}

	public static String getString(Context context, String key, String defaultValue) {
		if (context != null) {
			try {
				return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
			}
			catch (ClassCastException e) {
				L.e("ClassCastException: " + e.getMessage());
			}
		}
		else {
			L.w("Context is null");
		}
		return "";
	}

	public static Boolean putString(Context context, String key, String value) {
		if (context != null) {
			L.d(key + " = " + value);
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putString(key, value);
			return editor.commit();
		}
		else {
			L.w("Context is null");
		}
		return false;
	}

	public static Long getListeningPort(Context context)
	{
		String listeningPort = LocalPreferences.getString(context,
				LocalPreferences.PREF_PORT,
				LocalPreferences.PREF_PORT_DEFAULT);
		Long port = null;

		try
		{
			port = Long.getLong(listeningPort, 2222);
		}
		catch(NumberFormatException e)
		{
			port = 2222L;
		}

		if (port < LocalPreferences.PORT_MIN
				|| port > LocalPreferences.PORT_MAX)
		{
			port = 2222L;
		}

		return port;
	}

	public static String getLocalFilesDir(Context context)
	{
		return RootUtils.hasRootAccess
				? ServerUtils.getLocalDir(context)
				: "/tmp";
	}
}
