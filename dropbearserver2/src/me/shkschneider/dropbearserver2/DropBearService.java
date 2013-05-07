package me.shkschneider.dropbearserver2;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import me.shkschneider.dropbearserver2.util.L;
import me.shkschneider.dropbearserver2.util.RootUtils;
import me.shkschneider.dropbearserver2.util.ServerUtils;
import me.shkschneider.dropbearserver2.util.ShellUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

public class DropBearService extends Service
{
	public static final String STATECHANGED_INTENT = "me.shkschneider.dropbearserver2.intent.STATE";
	public static final String SERVICEMANAGE_INTENT = "me.shkschneider.dropbearserver2.intent.MANAGE";
	public static final String REQUESTCOUNT_INTENT = "me.shkschneider.dropbearserver2.intent.TRAFFIC";

	private static final CharSequence SERVICE_NAME = "SSH Service";
	private static final CharSequence SERVICE_RUNNING = "Running";

	private static final int ID_ROOT = 0;

	private static DropBearService mSingleton = null;

	// Qaching states
	public enum State
	{
		/* DropBear States */
		RUNNING, IDLE, STARTING, STOPPING, RESTARTING, FAILURE_LOG, FAILURE_EXE,

		/* Service states */
		STARTED, START, STOPPED, STOP;

		public boolean isServiceRunning()
		{
			return 	this.equals(STARTED) || this.equals(START);
		}

		public boolean isServerRunning()
		{
			return 	this.equals(RUNNING) || this.equals(STARTING) || this.equals(RESTARTING);
		}
	}

	// Default state
	private static State mState = State.IDLE;

	private final Binder mBinder = new LocalBinder();

	// Notification
	private NotificationManager mNotificationManager = null;
	private Notification mNotification = null;

	// Wifi performance
	private static WifiLock mWifiLock = null;
    private static WakeLock mWakeLock = null;

	// Intents
	private PendingIntent mMainIntent = null;

	// Set foreground
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private Object[] mSetForegroundArgs = new Object[1];

	private static final Class<?>[] mSetForegroundSignature = new Class[]
		{
			boolean.class
		};

	private static final Class<?>[] mStartForegroundSignature = new Class[]
		{
				int.class, Notification.class
		};

	private static final Class<?>[] mStopForegroundSignature = new Class[]
		{
			boolean.class
		};

	@SuppressWarnings("unused")
	private static IBinder getService(String service) throws Exception
	{
		Class<?> ServiceManager = Class.forName("android.os.ServiceManager");
		Method getService_method = ServiceManager.getMethod("getService",
				new Class[]
					{
						String.class
					});
		IBinder b = (IBinder) getService_method.invoke(null, new Object[]
			{
				service
			});
		return b;
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder
	{
		DropBearService getService()
		{
			return DropBearService.this;
		}
	}

	private void invokeMethod(Method method, Object[] args)
	{
		try
		{
			method.invoke(this, args);
		}
		catch (InvocationTargetException e)
		{
			// Should not happen.
			L.w("Unable to invoke method" + e.getMessage());
		}
		catch (IllegalAccessException e)
		{
			// Should not happen.
			L.w("Unable to invoke method" + e);
		}
	}

	private void initForeground()
	{
		try
		{
			mStartForeground = getClass().getMethod("startForeground",
					mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground",
					mStopForegroundSignature);
			return;
		}
		catch (NoSuchMethodException e)
		{
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}

		try
		{
			mSetForeground = getClass().getMethod("setForeground",
					mSetForegroundSignature);
		}
		catch (NoSuchMethodException e)
		{
			throw new IllegalStateException(
					"OS doesn't have Service.startForeground OR Service.setForeground!");
		}
	}

	private void handleCommand(Intent intent)
	{
		if (intent != null)
		{
			start();
		}
	}

	@SuppressWarnings("deprecation")
	private NotificationManager getNotificationManager()
	{
		if (mNotificationManager == null)
		{
			Context appContext = this.getApplicationContext();

			// init notificationManager
			mNotificationManager = (NotificationManager)
					appContext.getSystemService(Context.NOTIFICATION_SERVICE);
	    	mNotification = new Notification(R.drawable.start_notification,
	    			"DropBearService2", System.currentTimeMillis());
	
			mMainIntent = PendingIntent.getActivity(appContext,
					0, new Intent(appContext, MainActivity.class), 0);
		}

		return mNotificationManager;
	}

	// Notification
	@SuppressWarnings("deprecation")
	private Notification getStartNotification()
	{
		// Assure the notification objects exist
		this.getNotificationManager();

		// Setup this notification
		mNotification.flags = Notification.FLAG_ONGOING_EVENT;
		mNotification.setLatestEventInfo(this, SERVICE_NAME,
				SERVICE_RUNNING, mMainIntent);

		// Send the notification
		mNotificationManager.notify(-1, mNotification);

		return mNotification;
	}

	private WifiLock getWifiLock()
	{
		try
		{
			WifiManager wifiManager = (WifiManager)
					this.getSystemService(Context.WIFI_SERVICE);
			if (wifiManager == null)
			{
				Toast.makeText(this,
						"Could not get WifiManager!",
						Toast.LENGTH_SHORT).show();
			}
			else if (wifiManager.isWifiEnabled())
			{
				mWifiLock = wifiManager.createWifiLock(
						WifiManager.WIFI_MODE_FULL_HIGH_PERF,
						MainActivity.class.getName());
				if (mWifiLock == null)
				{
					Toast.makeText(this, "Could not get WifiLock!", Toast.LENGTH_SHORT).show();
				}

		        mWakeLock = ((PowerManager) this.getSystemService(Context.POWER_SERVICE))
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WlanSilencerWakeLock");
				if (mWakeLock == null)
				{
					Toast.makeText(this, "Could not get WakeLock!", Toast.LENGTH_SHORT).show();
				}

			}
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Could not lock Wifi: " + e.getMessage(),
					Toast.LENGTH_SHORT).show();
		}

		return mWifiLock;
	}

	private void startWifiLock()
	{
		if (mWifiLock == null)
		{
			getWifiLock();
			if (mWifiLock != null)
			{
				mWifiLock.setReferenceCounted(false);
				mWifiLock.acquire();
			}
		}
		if (mWakeLock != null)
		{
			mWakeLock.setReferenceCounted(false);
			mWakeLock.acquire();
		}
	}

	private void stopWifiLock()
	{
		if (mWifiLock != null)
		{
			mWifiLock.release();
		}
		if (mWakeLock != null)
		{
			mWakeLock.release();
		}
	}

	private Boolean startDropBear()
	{
		// stop any existing instance started by this service
		stopDropBear();

		// start a fresh copy with the current settings
		String localDir = LocalPreferences.getLocalFilesDir(this);
		String login = "root";
		String passwd = LocalPreferences.getString(this, LocalPreferences.PREF_PASSWORD, LocalPreferences.PREF_PASSWORD_DEFAULT);
		// String banner = localDir + "/banner";
		String hostRsa = localDir + "/host_rsa";
		String hostDss = localDir + "/host_dss";
		String authorizedKeys = localDir + "/authorized_keys";
		Long listeningPort = LocalPreferences.getListeningPort(this);
		String pidFile = localDir + "/pid";

		String command = localDir + "/dropbear";
		command = command.concat(" -A -N " + login);
		if (LocalPreferences.getBoolean(this, LocalPreferences.PREF_ALLOW_PASSWORD, LocalPreferences.PREF_ALLOW_PASSWORD_DEFAULT) == false) {
			command = command.concat(" -s ");
		}
		command = command.concat(" -C " + passwd);
		command = command.concat(" -r " + hostRsa + " -d " + hostDss);
		command = command.concat(" -R " + authorizedKeys);
		command = command.concat(" -p " + listeningPort);
		command = command.concat(" -P " + pidFile);

		if (RootUtils.hasRootAccess)
		{
			command = command.concat(" -U " + ID_ROOT + " -G " + ID_ROOT);
		}

		// command = command.concat(" -b " + banner);

		// Lock the wifi on if possible
		startWifiLock();

		// Start the service here.
		L.d("Command: " + command);

		if (ShellUtils.execute(command))
		{
			mState = ServerUtils.isDropbearRunning() ?
					State.RUNNING : State.FAILURE_LOG;
		}
		else
		{
			mState = State.FAILURE_EXE;
		}

		return mState.isServerRunning();
	}

	private Boolean stopDropBear()
	{
		Boolean bReturn = true;

		// kill the wifilock
		stopWifiLock();

		// kill any processes
		String localDir = LocalPreferences.getLocalFilesDir(this);
		File pidFile = new File(localDir + "/pid");

		if (pidFile.exists())
		{
			L.i("Killing process by pid file" + pidFile);
			bReturn = ShellUtils.killPidFile(pidFile.getAbsolutePath())
					&& ShellUtils.deleteFile(pidFile.getAbsolutePath());
		}

		// Really? This means we kill *any* dropbear server.
		//bReturn = ShellUtils.killall("dropbear");

		return bReturn;
	}

	/**
	 * Starts the dropbear server
	 */
	private void start()
	{
		mState = State.STARTING;
		sendStateBroadcast(mState.ordinal());

		new Thread(new Runnable()
		{
			public void run()
			{
				// Start the dropbear daemon
				DropBearService.this.startDropBear();
				DropBearService.this.sendStateBroadcast(mState.ordinal());
			}
		}).start();

		this.startForegroundCompat(-1, this.getStartNotification());
	}

	/*
	 * Stops the service and underlying processes.
	 */
	private void stop()
	{
		mState = State.STOPPING;
		sendStateBroadcast(mState.ordinal());

		new Thread(new Runnable()
		{
			public void run()
			{
				DropBearService.this.stopDropBear();

				// Check for failed-state
				if (mState == State.FAILURE_EXE)
				{
					mState = State.IDLE;
				}

				DropBearService.this.sendStateBroadcast(mState.ordinal());
				DropBearService.this.sendManageBroadcast(State.STOPPED.ordinal());
			}
		}).start();

		DropBearService.this.stopForegroundCompat(-1);
	}

	/*************************************************************************
	 * Override methods
	 */
	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	@Override
	public void onCreate()
	{
		L.d("============= DropBearService started! =============");

		// Init foreground
		this.initForeground();

		// Initialize itself
		if (mSingleton == null)
		{
			mSingleton = this;

			// Send a "state" broadcast -- we start out idle (presumed)
			this.sendStateBroadcast(State.IDLE.ordinal());
			mState = State.IDLE;
		}
	}

	@Override
	public void onDestroy()
	{
		L.d("============= DropBearService stopped! =============");
		stop();
		mSingleton = null;
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		this.handleCommand(intent);

		/* Stay alive */
		return START_STICKY;
	}

	/*************************************************************************
	 * Protected methods
	 */

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	protected void startForegroundCompat(int id, Notification notification)
	{
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null)
		{
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			this.invokeMethod(mStartForeground, mStartForegroundArgs);
			return;
		}
		else
		{
			// Fall back on the old API.
			mSetForegroundArgs[0] = Boolean.TRUE;
			this.invokeMethod(mSetForeground, mSetForegroundArgs);
			this.getNotificationManager().notify(id, notification);
		}
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	protected void stopForegroundCompat(int id)
	{
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null)
		{
			mStopForegroundArgs[0] = Boolean.TRUE;
			this.invokeMethod(this.mStopForeground, mStopForegroundArgs);
			return;
		}
		else
		{
			/*
			 * Fall back on the old API. Note to cancel BEFORE changing the
			 * foreground state, since we could be killed at that point.
			 */
			this.getNotificationManager().cancel(id);
			mSetForegroundArgs[0] = Boolean.FALSE;
			this.invokeMethod(mSetForeground, mSetForegroundArgs);
		}
	}

	/*************************************************************************
	 * Public methods
	 */

	public static DropBearService getSingleton()
	{
		return mSingleton;
	}

	public static Boolean isServiceRunning()
	{
		return (mSingleton != null);
	}

	public static Boolean isServerRunning()
	{
		return (isServiceRunning() && ServerUtils.isDropbearRunning());
	}

	public void sendStateBroadcast(int state)
	{
		Intent intent = new Intent(STATECHANGED_INTENT);
		intent.setAction(STATECHANGED_INTENT);
		intent.putExtra("state", state);
		this.sendBroadcast(intent);
	}

	public void sendManageBroadcast(int state)
	{
		Intent intent = new Intent(SERVICEMANAGE_INTENT);
		intent.setAction(SERVICEMANAGE_INTENT);
		intent.putExtra("state", state);
		this.sendBroadcast(intent);
	}
}
