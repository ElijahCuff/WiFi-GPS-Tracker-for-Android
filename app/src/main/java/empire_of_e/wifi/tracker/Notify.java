package empire_of_e.wifi.tracker;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class Notify extends Service { 

		static int notifyID = 6272;
		static String title;
		static String descr;
		static Intent notInt;
		static NotificationCompat.Builder builder;
		static NotificationManager notiManager;

		@Override
		public void onDestroy() {
				notiManager.cancel(notifyID);
				super.onDestroy();
		}


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        this.stopSelf();
				super.onTaskRemoved(rootIntent);
    }

		@Override
		public void onLowMemory() {
				// TODO: Implement this method
				//	super.onLowMemory();
		}

		@Override
		public void onTrimMemory(int level) {
				// TODO: Implement this method
				//super.onTrimMemory(level);
		}

		@Override
		public void onConfigurationChanged(Configuration newConfig) {
				// TODO: Implement this method
				//super.onConfigurationChanged(newConfig);
		}




		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
				builder =
            new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_launcher)
						.setAutoCancel(false)
						.setColorized(true)
						.setOngoing(true)
						//	.setContentTitle(title)
						.setTicker(getString(R.string.app_name))
						.setChannelId("zoopy")
						.setContentText(descr);

				Intent targetIntent = new Intent(this, MainActivity.class);
				PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_NO_CREATE);
				builder.setContentIntent(contentIntent);
				notiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				notiManager.notify(notifyID, builder.build());
				return START_STICKY;	
		}

		static int numMessages = 0;
		public static void updateNotification(String description) {

				builder.setContentText(description)
						.setNumber(++numMessages);
				// Because the ID remains unchanged, the existing notification is
				// updated.
				notiManager.notify(
						notifyID,
						builder.build());
		}

		@Override
		public IBinder onBind(Intent p1) {
				// TODO: Implement this method
				return null;
		}

}

