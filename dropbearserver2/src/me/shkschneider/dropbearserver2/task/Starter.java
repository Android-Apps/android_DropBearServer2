/*
 * Pawel Nadolski <http://stackoverflow.com/questions/10319471/android-is-the-groupid-of-sdcard-rw-always-1015/>
 */
package me.shkschneider.dropbearserver2.task;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import me.shkschneider.dropbearserver2.DropBearService;

public class Starter extends Task {

	public Starter(Context context, Callback<Boolean> callback, Boolean startInBackground) {
		super(Callback.TASK_START, context, callback, startInBackground);

		if (mProgressDialog != null) {
			mProgressDialog.setTitle("Starter");
		}
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		Boolean bStarted = false;

		// First, check if the service already exists
		if (DropBearService.isServiceRunning())
		{
			publishProgress("Started server");
			bStarted = true;
		}
		else
		{
			Intent intent = new Intent();
			intent.setClass(mContext, DropBearService.class);
			publishProgress("Starting service");
	
			ComponentName cn = null;
			try
			{
				cn = mContext.startService(intent);
				bStarted = cn != null;
				if (!bStarted)
				{
					publishProgress("Service failed to start!");
				}
			}
			catch (SecurityException s)
			{
				publishProgress("Service failed: " + s.getLocalizedMessage());
			}
		}

		return bStarted;
	}
}
