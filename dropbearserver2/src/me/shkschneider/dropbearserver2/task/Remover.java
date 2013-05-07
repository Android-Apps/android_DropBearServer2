package me.shkschneider.dropbearserver2.task;

import android.content.Context;

import me.shkschneider.dropbearserver2.LocalPreferences;
import me.shkschneider.dropbearserver2.util.RootUtils;
import me.shkschneider.dropbearserver2.util.ShellUtils;
import me.shkschneider.dropbearserver2.util.Utils;

public class Remover extends Task {

	public Remover(Context context, Callback<Boolean> callback) {
		super(Callback.TASK_REMOVE, context, callback, false);

		if (mProgressDialog != null) {
			mProgressDialog.setTitle("Remover");
		}
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		String localDir = LocalPreferences.getLocalFilesDir(mContext);

		publishProgress("Dropbear binary");
		ShellUtils.deleteFile(localDir + "/dropbear");

		publishProgress("Dropbearkey binary");
		ShellUtils.deleteFile(localDir + "/dropbearkey");

		removeClientApps();

		publishProgress("Banner");
		ShellUtils.deleteFile(localDir + "/banner");

		publishProgress("Authorized keys");
		ShellUtils.deleteFile(localDir + "/authorized_keys");

		publishProgress("Host RSA key");
		ShellUtils.deleteFile(localDir + "/host_rsa");

		publishProgress("Host DSS key");
		ShellUtils.deleteFile(localDir + "/host_dss");

		return true;
	}

	private Boolean removeClientApps()
	{
		boolean bReturn = true;

		if (RootUtils.hasRootAccess)
		{
			publishProgress("Remount Read-Write");
			if (Utils.remountReadWrite("/system") == false) {
				bReturn = falseWithError("/system RW");
			}
		}

		try
		{
			String xbin = null;

			if (RootUtils.hasRootAccess)
			{
				xbin = "/system/xbin";
			}
			else
			{
				xbin = "/tmp";
			}


			publishProgress("SSH binary");
			if (!ShellUtils.deleteFile(xbin + "/ssh"))
			{
				throw new Exception("Failed to remove ssh!");
			}

			publishProgress("SCP binary");
			if (!ShellUtils.deleteFile(xbin + "/scp"))
			{
				throw new Exception("Failed to remove scp!");
			}

			publishProgress("DBClient binary");
			if (!ShellUtils.deleteFile(xbin + "/dbclient"))
			{
				throw new Exception("Failed to remove dbclient!");
			}

			publishProgress("SFTP binary");
			if (!ShellUtils.deleteFile(xbin + "/sftp-server"))
			{
				throw new Exception("Failed to remove sftp-server!");
			}
		}
		catch (Exception e)
		{
			publishProgress("Exception: " + e.getMessage());
			bReturn = falseWithError("Exception: " + e.getMessage());
		}

		if (RootUtils.hasRootAccess)
		{
			publishProgress("Remount Read-Only");
			if (Utils.remountReadOnly("/system") == false) {
				bReturn = falseWithError("/system RO");
			}
		}

		return bReturn;
	}
}
