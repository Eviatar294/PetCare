package com.example.petcare;

import android.app.Application;

public class CreateNotificationChannel extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Create the notification channel only once when the app starts.
        TaskReminderReceiver.createNotificationChannel(this);
    }
}
