package com.example.petcare;

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

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("TaskReminderReceiver", "🔔 Broadcast Receiver Triggered!");

        // Ensure notification channels exist before trying to send notifications
        PetCareApplication.ensureNotificationChannelsExist(context);

        // Get the task list from the intent
        String taskList = intent.getStringExtra("taskList");

        if (taskList == null || taskList.isEmpty()) {
            taskList = "Assign to the tasks of tomorrow!";
            Log.w("TaskReminderReceiver", "⚠️ Task list was empty, using default message.");
        } else {
            Log.d("TaskReminderReceiver", "📌 Task List Received: " + taskList);
        }

        // Intent to open the app when notification is clicked
        Intent activityIntent = new Intent(context, MainHomeUser.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification using the correct channel ID
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PetCareApplication.TASK_REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("🐾 PetCare Task Reminder")
                .setContentText(taskList)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(taskList))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Send the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // Android 13+
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    NotificationManagerCompat.from(context).notify(1, builder.build());
                    Log.d("TaskReminderReceiver", "✅ Notification Sent! (Android 13+)");
                } catch (Exception e) {
                    Log.e("TaskReminderReceiver", "❌ Failed to send notification: " + e.getMessage());
                }
            } else {
                Log.e("TaskReminderReceiver", "🚨 POST_NOTIFICATIONS permission NOT granted!");
            }
        } else {
            try {
                NotificationManagerCompat.from(context).notify(1, builder.build());
                Log.d("TaskReminderReceiver", "✅ Notification Sent! (pre-Android 13)");
            } catch (Exception e) {
                Log.e("TaskReminderReceiver", "❌ Failed to send notification: " + e.getMessage());
            }
        }
    }
}
