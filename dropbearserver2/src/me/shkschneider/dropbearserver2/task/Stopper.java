package me.shkschneider.dropbearserver2.task;

import android.content.Context;
import android.content.Intent;

import me.shkschneider.dropbearserver2.DropBearService;

public class Stopper extends Task {

	public Stopper(Context context, Callback<Boolean> callback, boolean startInBackground) {
		super(Callback.TASK_STOP, context, callback, startInBackground);

		if (mProgressDialog != null) {
			mProgressDialog.setTitle("Stopper");
		}
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		Intent intent = new Intent();
		intent.setClass(mContext, DropBearService.class);
		publishProgress("Stopping service");

		boolean bStopped = false;
		try
		{
			bStopped = mContext.stopService(intent);

			if (!bStopped)
			{
				publishProgress("Service failed to stop!");
			}
		}
		catch (SecurityException s)
		{
			publishProgress("Stop failed: " + s.getLocalizedMessage());
			bStopped = false;
		}

		return true;
	}
}