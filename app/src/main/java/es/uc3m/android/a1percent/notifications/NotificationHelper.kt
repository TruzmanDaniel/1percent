package es.uc3m.android.a1percent.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import es.uc3m.android.a1percent.MainActivity
import es.uc3m.android.a1percent.R

object NotificationHelper {

    // Channel IDs
    const val CHANNEL_SOCIAL = "social"
    const val CHANNEL_REMINDERS = "reminders"

    // Intent extras — read by MainActivity to know where to navigate on tap
    const val EXTRA_NAVIGATE_TO = "navigate_to"
    const val NAV_SOCIAL = "social?section=friends"

    // Notification IDs
    private const val ID_FRIEND_REQUEST = 1001
    private const val ID_FRIEND_ACCEPTED = 1002
    private const val ID_DAILY_REMINDER = 2001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val socialChannel = NotificationChannel(
            CHANNEL_SOCIAL,
            "Social",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Friend requests and social activity"
        }

        val remindersChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily mission reminders and streak alerts"
        }

        manager.createNotificationChannel(socialChannel)
        manager.createNotificationChannel(remindersChannel)
    }

    // Generic intent — brings the app to the foreground without forcing a destination.
    // FLAG_SINGLE_TOP: if the activity is already running, calls onNewIntent instead of recreating.
    // FLAG_CLEAR_TASK removed: we no longer want to kill the running app on tap.
    private fun mainActivityIntent(context: Context, navigateTo: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (navigateTo != null) putExtra(EXTRA_NAVIGATE_TO, navigateTo)
        }
        // Use a different request code when a navigation target is set so the PendingIntents
        // are distinct and Android does not strip the extra when reusing a cached one.
        val requestCode = if (navigateTo != null) 1 else 0
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @Suppress("MissingPermission")
    fun notifyFriendRequest(context: Context, fromName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New friend request")
            .setContentText("$fromName wants to be your friend")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(mainActivityIntent(context, NAV_SOCIAL))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ID_FRIEND_REQUEST, notification)
    }

    @Suppress("MissingPermission")
    fun notifyFriendAccepted(context: Context, byName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Friend request accepted")
            .setContentText("$byName accepted your friend request")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(mainActivityIntent(context, NAV_SOCIAL))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ID_FRIEND_ACCEPTED, notification)
    }

    @Suppress("MissingPermission")
    fun notifyDailyReminder(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Your missions are waiting")
            .setContentText("Don't forget your 1% today. Keep the streak alive!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(mainActivityIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ID_DAILY_REMINDER, notification)
    }
}
