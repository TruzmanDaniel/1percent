package es.uc3m.android.a1percent.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver triggered when the device finishes booting.
 * AlarmManager alarms are wiped on reboot, so we reschedule here.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.scheduleDailyReminder(context)
        }
    }
}
