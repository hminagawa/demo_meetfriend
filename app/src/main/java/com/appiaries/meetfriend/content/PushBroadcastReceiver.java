package com.appiaries.meetfriend.content;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.appiaries.meetfriend.R;

/**
 * BroadcastReceviver to receive Push Notification.
 */
public class PushBroadcastReceiver extends BroadcastReceiver {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        // Setting the notification
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Icon
        builder.setSmallIcon(R.drawable.ic_launcher);
        // Ticker text (for the status bar)
        builder.setTicker("Updates from MeetFriend!");
        // Title for the notification.
        builder.setContentTitle(intent.getStringExtra("title"));
        // Text for the notification.
        builder.setContentText(intent.getStringExtra("message"));
        // Optional settings to let the message disappear when tapping.
        builder.setAutoCancel(true);

        // Intent issued when tapped.
        Intent newIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(intent.getStringExtra("url")));
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        // Sound and vibration.
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        // Creating NotificationManager.
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }

}