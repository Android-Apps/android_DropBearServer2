package me.shkschneider.dropbearserver2.task;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;

import me.shkschneider.dropbearserver2.LocalPreferences;
import me.shkschneider.dropbearserver2.R;
import me.shkschneider.dropbearserver2.util.RootUtils;
import me.shkschneider.dropbearserver2.util.ServerUtils;
import me.shkschneider.dropbearserver2.util.ShellUtils;
import me.shkschneider.dropbearserver2.util.Utils;

public class Installer extends Task {

	public Installer(Context context, Callback<Boolean> callback) {
		super(Callback.TASK_INSTALL, context, callback, false);

		if (mProgressDialog != null) {
			mProgressDialog.setTitle("Installer");
		}
	}

	@SuppressLint("NewApi")
	private Boolean copyToAppData(int resId, String path, boolean bExecutable) {
		if (new File(path).exists() == true
				&& ShellUtils.deleteFile(path) == false) {
			return falseWithError(path);
		}
		if (Utils.copyRawFile(mContext, resId, path) == false) {
			return falseWithError(path);
		}

		if (RootUtils.hasRootAccess)
		{
			if (ShellUtils.chmod(path, "755") == false) {
				return falseWithError(path);
			}
		}
		else
		{
			File pathFile = new File(path);
			pathFile.setReadable(true, false);
			pathFile.setWritable(true, true);
			if (bExecutable)
			{
				pathFile.setExecutable(true, false);
			}
		}

		return true;
	}

	@SuppressLint("NewApi")
	private Boolean copyToSystemXbin(int resId, String tmp, String path) {
		Boolean bReturn = true;

		if (RootUtils.hasRootAccess)
		{
			if (Utils.copyRawFile(mContext, resId, tmp) == false) {
				bReturn = falseWithError(tmp);
			}
			if (ShellUtils.deleteFile(path) == false) {
				// Ignore
			}
			if (ShellUtils.cp(tmp, path) == false) {
				bReturn = falseWithError(path);
			}
			if (ShellUtils.deleteFile(tmp) == false) {
				// Ignore
			}
			if (ShellUtils.chmod(path, "755") == false) {
				bReturn = falseWithError(path);
			}
		}
		else
		{
			bReturn = copyToAppData(resId, path, true);
		}

		return bReturn;
	}

	@SuppressLint("NewApi")
	@Override
	protected Boolean doInBackground(Void... params) {
		String localDir = LocalPreferences.getLocalFilesDir(mContext);
		String tmp = localDir;
		String xbin = localDir;

		if (RootUtils.hasRootAccess)
		{
			tmp = localDir + "/tmp";
			xbin = "/system/xbin";
		}

		publishProgress("Dropbear binary");
		copyToAppData(R.raw.dropbear, localDir + "/dropbear", true);

		publishProgress("Dropbearkey binary");
		copyToAppData(R.raw.dropbearkey, localDir + "/dropbearkey", true);

		if (RootUtils.hasRootAccess)
		{
			publishProgress("Remount Read-Write");
			if (Utils.remountReadWrite("/system") == false) {
				return falseWithError("/system RW");
			}
		}

		publishProgress("SSH binary");
		copyToSystemXbin(R.raw.ssh, tmp, xbin + "/ssh");

		publishProgress("SCP binary");
		copyToSystemXbin(R.raw.scp, tmp, xbin + "/scp");

		publishProgress("DBClient binary");
		copyToSystemXbin(R.raw.dbclient, tmp, xbin + "/dbclient");

		publishProgress("SFTP binary");
		copyToSystemXbin(R.raw.sftp_server, tmp, xbin + "/sftp-server");

		if (RootUtils.hasRootAccess)
		{
			publishProgress("Remount Read-Only");
			if (Utils.remountReadOnly("/system") == false) {
				return falseWithError("/system RO");
			}

			publishProgress("Permissions");
			if (ShellUtils.chmod("/data/local", "755") == false) {
				return falseWithError("/data/local");
			}
		}

		publishProgress("Banner");
		copyToAppData(R.raw.banner, localDir + "/banner", false);

		publishProgress("Authorized keys");
		String authorized_keys = localDir + "/authorized_keys";
		if (new File(authorized_keys).exists()
				&& !ShellUtils.deleteFile(authorized_keys)) {
			return falseWithError(authorized_keys);
		}
		if (ServerUtils.createIfNeeded(authorized_keys) == false) {
			return falseWithError(authorized_keys);
		}

		publishProgress("Host RSA key");
		String host_rsa = localDir + "/host_rsa";
		File keyFile = new File(host_rsa);
		if (keyFile.exists() && !ShellUtils.deleteFile(host_rsa))
		{
			return falseWithError(host_rsa);
		}
		if (ServerUtils.generateRsaPrivateKey(host_rsa) == false) {
			return falseWithError(host_rsa);
		}

		publishProgress("Host DSS key");
		String host_dss = localDir + "/host_dss";
		keyFile = new File(host_dss);
		if (keyFile.exists() && !ShellUtils.deleteFile(host_dss)) {
			falseWithError(host_dss);
		}
		if (ServerUtils.generateDssPrivateKey(host_dss) == false) {
			return falseWithError(host_dss);
		}

		if (RootUtils.hasRootAccess)
		{
			if (ShellUtils.chmod(authorized_keys, "644") == false) {
				return falseWithError(authorized_keys);
			}

			if (ShellUtils.chown(host_rsa, "0:0") == false) {
				return falseWithError(host_rsa);
			}
			if (ShellUtils.chown(host_dss, "0:0") == false) {
				return falseWithError(host_dss);
			}
		}
		else
		{
			File pathFile = new File(authorized_keys);
			pathFile.setReadable(true, false);
			pathFile.setWritable(true, true);
			pathFile.setExecutable(false);
		}

		return true;
	}
}