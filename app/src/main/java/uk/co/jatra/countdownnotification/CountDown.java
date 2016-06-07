package uk.co.jatra.countdownnotification;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class CountDown extends Service {

    private Notification notification;
    public CountDown() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent intentDefault = new Intent(this, MainActivity.class);
//        intentDefault.putExtra("reservation", SessionManager.getReservationIndex());
        intentDefault.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intentDefault, PendingIntent.FLAG_UPDATE_CURRENT);
        notification = baseNotificationBuilder(this, pIntent).build();
        startForeground(101, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static NotificationCompat.Builder baseNotificationBuilder(Context context, PendingIntent pIntent) {
        return new NotificationCompat.Builder(context)

                .setContentTitle("Zipcar reservation in progress")
                .setContentText("Click here to open your zipcar reservation details")
                .setSmallIcon(android.R.drawable.ic_media_ff)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_VIBRATE) //makes the watches vibrate
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setGroup("GROUP")
                .setGroupSummary(true);
    }
}
