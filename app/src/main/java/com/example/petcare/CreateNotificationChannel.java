package com.example.petcare;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

public class CreateNotificationChannel extends Application {
    public static final String TASK_REMINDER_CHANNEL_ID = "TaskReminderChannel";
    public static final String RECURRING_TASK_CHANNEL_ID = "RecurringTaskChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) {
                Log.e("CreateNotificationChannel", "NotificationManager is null!");
                return;
            }

            // Check if channels already exist
            NotificationChannel existingTaskReminder = manager.getNotificationChannel(TASK_REMINDER_CHANNEL_ID);
            NotificationChannel existingRecurring = manager.getNotificationChannel(RECURRING_TASK_CHANNEL_ID);

            // Only create channels if they don't exist
            if (existingTaskReminder == null) {
                // Create Task Reminder Channel
                NotificationChannel taskReminderChannel = new NotificationChannel(
                        TASK_REMINDER_CHANNEL_ID,
                        "Task Reminder Channel",
                        NotificationManager.IMPORTANCE_HIGH
                );
                taskReminderChannel.setDescription("Notifications for unassigned pet tasks.");
                taskReminderChannel.setShowBadge(true);
                taskReminderChannel.enableVibration(true);
                taskReminderChannel.setBypassDnd(true);
                manager.createNotificationChannel(taskReminderChannel);
                Log.d("CreateNotificationChannel", "Task Reminder Channel Created!");
            }

            if (existingRecurring == null) {
                // Create Recurring Task Channel
                NotificationChannel recurringTaskChannel = new NotificationChannel(
                        RECURRING_TASK_CHANNEL_ID,
                        "Recurring Task Channel",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                recurringTaskChannel.setDescription("Notifications for recurring tasks.");
                recurringTaskChannel.setShowBadge(true);
                manager.createNotificationChannel(recurringTaskChannel);
                Log.d("CreateNotificationChannel", "Recurring Task Channel Created!");
            }
        }
    }

    /**
     * Helper method to ensure notification channels exist
     * Can be called from anywhere in the app if notifications aren't working
     */
    public static void ensureNotificationChannelsExist(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) {
                Log.e("CreateNotificationChannel", "NotificationManager is null!");
                return;
            }

            // Check if channels exist
            NotificationChannel taskReminder = manager.getNotificationChannel(TASK_REMINDER_CHANNEL_ID);
            NotificationChannel recurring = manager.getNotificationChannel(RECURRING_TASK_CHANNEL_ID);

            // Recreate channels if they don't exist
            if (taskReminder == null) {
                NotificationChannel channel = new NotificationChannel(
                        TASK_REMINDER_CHANNEL_ID,
                        "Task Reminder Channel",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Notifications for unassigned pet tasks.");
                channel.setShowBadge(true);
                channel.enableVibration(true);
                channel.setBypassDnd(true);
                manager.createNotificationChannel(channel);
                Log.d("CreateNotificationChannel", "Task Reminder Channel Recreated!");
            }

            if (recurring == null) {
                NotificationChannel channel = new NotificationChannel(
                        RECURRING_TASK_CHANNEL_ID,
                        "Recurring Task Channel",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Notifications for recurring tasks.");
                channel.setShowBadge(true);
                manager.createNotificationChannel(channel);
                Log.d("CreateNotificationChannel", "Recurring Task Channel Recreated!");
            }
        }
    }
}
