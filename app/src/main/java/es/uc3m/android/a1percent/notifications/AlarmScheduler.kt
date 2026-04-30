package es.uc3m.android.a1percent.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Manages scheduling and cancellation of the daily reminder alarm.
 *
 * Uses AlarmManager.setRepeating() with RTC_WAKEUP (slide 29):
 *  - RTC_WAKEUP  → fires at wall-clock time, wakes the device if asleep
 *  - INTERVAL_DAY → repeats every 24 hours automatically
 *
 * The first trigger is calculated as the next occurrence of 20:00 (8pm).
 * If 8pm has already passed today, it schedules for 8pm tomorrow.
 */
object AlarmScheduler {

    private const val REQUEST_CODE = 42

    fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Calculate next 8pm
        val triggerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If 8pm has already passed today, move to tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        /** For testing purposes, we can set the trigger time to 1 minute from now instead of 8pm:
        val triggerTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 2)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        */

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            buildPendingIntent(context)
        )
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    // Builds the PendingIntent that points to ReminderReceiver.
    // FLAG_UPDATE_CURRENT: if a pending intent already exists, update it instead of creating a new one.
    // FLAG_IMMUTABLE: required on Android 12+ for security.
    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
