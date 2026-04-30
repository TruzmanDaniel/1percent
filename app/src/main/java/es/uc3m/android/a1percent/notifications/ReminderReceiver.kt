package es.uc3m.android.a1percent.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver triggered by AlarmManager every day at the scheduled time.
 * Its only job is to fire the daily reminder notification.
 * Declared in AndroidManifest.xml so the OS knows it exists.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.notifyDailyReminder(context)
    }
}
