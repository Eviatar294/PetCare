package com.example.petcare;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class TaskNotificationScheduler {

    public static void scheduleTaskReminder(Context context) {
        String userId = readUserIdFromStorage(context);
        if (userId == null) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot ds) {
                if (!ds.exists()) {
                    return;
                }

                String petId = ds.child("petId").getValue(String.class);
                String notificationTime = ds.child("notificationTime")
                        .getValue(String.class);
                if (notificationTime == null || notificationTime.isEmpty()) {
                    notificationTime = "20:00";
                }
                if (petId == null || petId.isEmpty()) {
                    return;
                }

                fetchUnassignedTasks(context, petId, notificationTime);
            }

            @Override
            public void onCancelled(DatabaseError e) {
                // no-op
            }
        });
    }

    private static void fetchUnassignedTasks(
            Context context,
            String petId,
            String notificationTime
    ) {
        FirebaseDatabase.getInstance()
                .getReference("Tasks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snap) {
                        ArrayList<String> names = new ArrayList<>();
                        String tomorrow = getTomorrowDate();

                        for (DataSnapshot t : snap.getChildren()) {
                            Task task = t.getValue(Task.class);
                            if (task != null
                                    && petId.equals(task.getPetId())
                                    && tomorrow.equals(task.getDueDate())
                                    && "none".equalsIgnoreCase(task.getRecurrenceType())
                                    && (task.getAssignedUserId() == null
                                    || task.getAssignedUserId().trim().isEmpty())
                            ) {
                                names.add(task.getTaskName());
                            }
                        }

                        if (!names.isEmpty()) {
                            String list = "Unassigned Tasks for tomorrow: "
                                    + String.join(", ", names);
                            scheduleAlarm(context, list, notificationTime);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError e) {
                        // no-op
                    }
                });
    }

    private static void scheduleAlarm(
            Context context,
            String taskList,
            String notificationTime
    ) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, TaskReminderReceiver.class);
        intent.putExtra("taskList", taskList);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (am != null) {
            am.cancel(pi);
        }

        // Parse "HH:mm"
        String[] parts = notificationTime.split(":");
        int hour   = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        // Schedule for today at the chosen time, or tomorrow if that time has already passed
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        Calendar now = Calendar.getInstance();
        if (!cal.after(now)) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        long triggerAt = cal.getTimeInMillis();

        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pi
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExact(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pi
                    );
                }
            } else {
                am.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pi
                );
            }
        }
    }

    private static String getTomorrowDate() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, 1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(c.getTime());
    }

    private static String readUserIdFromStorage(Context ctx) {
        File f = new File(ctx.getFilesDir(), "user_id.txt");
        if (!f.exists()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            return br.readLine();
        } catch (Exception e) {
            return null;
        }
    }
}
