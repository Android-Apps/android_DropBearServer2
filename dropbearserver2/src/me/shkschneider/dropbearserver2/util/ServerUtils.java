/*
 * Sherif elKhatib <http://stackoverflow.com/questions/6896618/read-command-output-inside-su-process>
 * Martin <http://www.droidnova.com/get-the-ip-address-of-your-device,304.html>
 * javadb <http://www.javadb.com/remove-a-line-from-a-text-file>
 * external-ip <http://code.google.com/p/external-ip/>
 */
package me.shkschneider.dropbearserver2.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import me.shkschneider.dropbearserver2.LocalPreferences;

import org.apache.http.conn.util.InetAddressUtils;

import android.annotation.SuppressLint;
import android.content.Context;

public abstract class ServerUtils {

	public static String localDir = null;
	public static Boolean dropbearRunning = false;
	public static String dropbearVersion = null;
	public static List<String> ipAddresses = null;

	public static final String getLocalDir(Context context) {
		if (localDir == null) {
			localDir = context.getDir("data", Context.MODE_PRIVATE).toString();
		}
		return localDir;
	}

	// WARNING: this is not threaded
	public static final List<String> getIpAddresses(Context context) {
		if (ipAddresses == null) {
			ipAddresses = new ArrayList<String>();
			try {
				for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
					NetworkInterface intf = en.nextElement();
					for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						String ipAddress = inetAddress.getHostAddress().toString();
						if (InetAddressUtils.isIPv4Address(ipAddress) && !ipAddresses.contains(ipAddress)) {
							ipAddresses.add(ipAddress);
						}
					}
				}
			}
			catch (SocketException e) {
				L.e("SocketException: " + e.getMessage());
			}
			return ipAddresses;
		}
		return ipAddresses;
	}

	// WARNING: this is not threaded
	public static final Boolean isDropbearRunning() {
		dropbearRunning = false;
		try {
			String cmd = RootUtils.hasRootAccess ? "su" : "sh";
			Process process = Runtime.getRuntime().exec(cmd);

			// stdin
			DataOutputStream stdin = new DataOutputStream(process.getOutputStream());
			L.d("# ps dropbear");
			stdin.writeBytes("ps dropbear\n");
			stdin.flush();
			stdin.writeBytes("exit\n");
			stdin.flush();

			// stdout
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.endsWith("dropbear") == true) {
					dropbearRunning = true;
					return dropbearRunning;
				}
			}
		}
		catch (IOException e) {
			L.e("IOException: " + e.getMessage());
		}
		return dropbearRunning;
	}

	// WARNING: this is not threaded
	@SuppressLint("NewApi")
	public static final Boolean generateRsaPrivateKey(String path) {
		Boolean bReturn = false;

		if (RootUtils.hasRootAccess)
		{
			bReturn = ShellUtils.execute(
					LocalPreferences.getLocalFilesDir(null)
					+ "/dropbearkey -t rsa -f " + path);
		}
		else
		{
			try
			{
				File tmpFile = File.createTempFile("__db2", null, new File("/tmp"));
				File rsaFile = new File(path);
				tmpFile.setReadable(true,  false);
				tmpFile.setWritable(true,  false);
				tmpFile.setExecutable(false);
				String cmd = LocalPreferences.getLocalFilesDir(null)
						+ "/dropbearkey -t rsa -f " + tmpFile.getAbsolutePath();
				bReturn = ShellUtils.execute(cmd);
				if (bReturn)
				{
					// copy the file into place
					ShellUtils.moveFile(tmpFile.getAbsoluteFile(), rsaFile);
				}
			}
			catch(IOException ioe)
			{
				bReturn = false;
			}
		}

		return bReturn;
	}

	// WARNING: this is not threaded
	@SuppressLint("NewApi")
	public static final Boolean generateDssPrivateKey(String path) {
		Boolean bReturn = false;

		if (RootUtils.hasRootAccess)
		{
			bReturn = ShellUtils.execute(
					LocalPreferences.getLocalFilesDir(null)
					+ "/dropbearkey -t dss -f " + path);
		}
		else
		{
			File tmpFile = null;
	
			try
			{
				File dssFile = new File(path);
				tmpFile = File.createTempFile("__db2", null, new File("/tmp"));
				tmpFile.setReadable(true,  false);
				tmpFile.setWritable(true,  false);
				tmpFile.setExecutable(false);
				String cmd = LocalPreferences.getLocalFilesDir(null)
							+ "/dropbearkey -t dss -f " + tmpFile.getAbsolutePath();
				bReturn = ShellUtils.execute(cmd);
				if (bReturn)
				{
					// copy the file into place
					ShellUtils.moveFile(tmpFile, dssFile);
				}
			}
			catch(IOException ioe)
			{
				bReturn = false;
			}
			finally
			{
				if (tmpFile != null)
				{
					tmpFile.delete();
				}
			}
		}

		return bReturn;
	}

	// WARNING: this is not threaded
	public static List<String> getPublicKeys(String path) {
		List<String> publicKeys = new ArrayList<String>();
		File f = new File(path);
		if (f.exists() == true && f.isFile() == true) {
			try {
				FileInputStream fis = new FileInputStream(path);
				DataInputStream dis = new DataInputStream(fis);
				BufferedReader br = new BufferedReader(new InputStreamReader(dis));
				String line = null;
				while ((line = br.readLine()) != null) {
					publicKeys.add(line);
				}
				dis.close();
			}
			catch (FileNotFoundException e) {
				L.e("FileNotFoundException: " + e.getMessage());
			}
			catch (IOException e) {
				L.e("IOException: " + e.getMessage());
			}
		}
		else {
			L.w("File could not be found: " + path);
		}
		return publicKeys;
	}

	// WARNING: this is not threaded
	public static final Boolean addPublicKey(String publicKey, String path) {
		File f = new File(path);
		if (f.exists() == true && f.isFile() == true) {
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(path, true));
				out.write(publicKey + "\n");
				out.close();
				return true;
			}
			catch (IOException e) {
				L.e("IOException: " + e.getMessage());
			}
		}
		return false;
	}

	// WARNING: this is not threaded
	public static final Boolean removePublicKey(String publicKey, String path) {
		File f = new File(path);
		if (f.exists() == true && f.isFile() == true) {
			try {
				File inFile = new File(path);
				File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

				BufferedReader br = new BufferedReader(new FileReader(path));
				PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

				String line = null;

				while ((line = br.readLine()) != null) {
					if (!line.trim().equals(publicKey)) {
						pw.println(line);
						pw.flush();
					}
				}
				pw.close();
				br.close();

				if (!inFile.delete()) {
					L.w("delete() failed");
					return false;
				}
				if (!tempFile.renameTo(inFile)) {
					L.w("renameTo() failed");
					return false;
				}
				return true;
			}
			catch (FileNotFoundException e) {
				L.e("FileNotFoundException: " + e.getMessage());
			}
			catch (IOException e) {
				L.e("IOException: " + e.getMessage());
			}
		}
		return false;
	}

	public static final Boolean createIfNeeded(String path) {
		Boolean bReturn = false;

		File file = new File(path);
		if (file.exists() == false) {
			try {
				file.createNewFile();
				bReturn = true;
			}
			catch (IOException e) {
				L.e("IOException: " + e.getMessage());
			}
		}

		return bReturn;
	}

	// WARNING: this is not threaded
	public static final String getDropbearVersion(Context context) {
		dropbearVersion = null;
		try {
			String cmd = RootUtils.hasRootAccess ? "su" : "sh";
			Process suProcess = Runtime.getRuntime().exec(cmd);

			// stdin
			DataOutputStream stdin = new DataOutputStream(suProcess.getOutputStream());
			L.d("# dropbear -h");
			stdin.writeBytes(LocalPreferences.getLocalFilesDir(context)
					+ "/dropbear -h 2>&1 | head -n 1\n");
			stdin.flush();
			stdin.writeBytes("exit\n");
			stdin.flush();

			// stdout
			BufferedReader reader = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));
			String line = reader.readLine();

			// parsing
			if (line != null && line.matches("^Dropbear sshd v[0-9\\.]+$")) {
				dropbearVersion = line.replaceFirst("^Dropbear sshd v", "");
			}
		}
		catch (IOException e) {
			L.e("IOException: " + e.getMessage());
		}
		return dropbearVersion;
	}
}
