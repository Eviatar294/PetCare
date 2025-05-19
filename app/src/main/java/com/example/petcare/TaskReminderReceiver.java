package com.example.petcare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class TaskReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "TaskReminderChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("TaskReminderReceiver", "🔔 Broadcast Receiver Triggered!");

        // Get the task list from the intent
        String taskList = intent.getStringExtra("taskList");

        if (taskList == null || taskList.isEmpty()) {
            taskList = "Assign to the tasks of tomorrow!";
            Log.w("TaskReminderReceiver", "⚠️ Task list was empty, using default message.");
        } else {
            Log.d("TaskReminderReceiver", "📌 Task List Received: " + taskList);
        }

        // Do not create the notification channel here; it is created once in MyApplication.

        // Intent to open the app when notification is clicked
        Intent activityIntent = new Intent(context, MainHomeUser.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists in drawable
                .setContentTitle("🐾 PetCare Task Reminder")
                .setContentText(taskList)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(taskList))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Send the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // Android 13+
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(1, builder.build());
                Log.d("TaskReminderReceiver", "✅ Notification Sent! (Android 13+)");
            } else {
                Log.e("TaskReminderReceiver", "🚨 POST_NOTIFICATIONS permission NOT granted!");
            }
        } else {
            NotificationManagerCompat.from(context).notify(1, builder.build());
            Log.d("TaskReminderReceiver", "✅ Notification Sent! (pre-Android 13)");
        }
    }

    // The static method remains available, but it is now called once from MyApplication.onCreate().
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) {
                Log.e("TaskReminderReceiver", "🚨 NotificationManager is null, channel creation failed!");
                return;
            }
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Task Reminder Channel", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for unassigned pet tasks.");
            manager.createNotificationChannel(channel);
            Log.d("TaskReminderReceiver", "✅ Notification Channel Created!");
        }
    }
}
