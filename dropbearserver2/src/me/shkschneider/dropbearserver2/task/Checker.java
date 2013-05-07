package me.shkschneider.dropbearserver2.task;

import android.content.Context;

import me.shkschneider.dropbearserver2.DropBearService;
import me.shkschneider.dropbearserver2.LocalPreferences;
import me.shkschneider.dropbearserver2.util.RootUtils;
import me.shkschneider.dropbearserver2.util.ServerUtils;

public class Checker extends Task {

	public Checker(Context context, Callback<Boolean> callback) {
		super(Callback.TASK_CHECK, context, callback, true);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		Long listeningPort = LocalPreferences.getListeningPort(mContext);

		RootUtils.checkRootAccess();
		RootUtils.checkDropbear(mContext);

		Boolean bServerRunning = DropBearService.isServiceRunning();

		if (RootUtils.hasDropbear == true) {
			ServerUtils.getDropbearVersion(mContext);
		}
		if (bServerRunning) {
			ServerUtils.getIpAddresses(mContext);
		}

		Boolean bAPISupport = RootUtils.hasRootAccess
				|| android.os.Build.VERSION.SDK_INT >
					android.os.Build.VERSION_CODES.FROYO;

		return (((listeningPort > 1024L) || RootUtils.hasRootAccess)
				&& RootUtils.hasDropbear && bServerRunning && bAPISupport);
	}
}