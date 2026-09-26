package com.kkfittracking.watch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.kkfittracking.wear.WatchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * An ongoing activity while a workout runs, like the watch's own workout app: an icon on the watch
 * face and in the recent apps with the exercise to do, one tap back into the app. Keeps the app
 * alive in the background; stops when the workout ends.
 */
class WatchWorkoutService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ongoing: OngoingActivity? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = WatchSession.state.value
        val builder = notification(statusText(state))
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, builder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (ongoing == null) {
            ongoing = OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
                .setStaticIcon(R.drawable.ic_workout)
                .setTouchIntent(openApp())
                .setStatus(Status.Builder().addTemplate(statusText(state)).build())
                .build()
                .also { it.apply(applicationContext) }
            scope.launch {
                WatchSession.state
                    .map { it?.takeIf { s -> s.active }?.let(::statusText) }
                    .distinctUntilChanged()
                    .collect { text ->
                        if (text == null) {
                            ServiceCompat.stopForeground(this@WatchWorkoutService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        } else {
                            ongoing?.update(applicationContext, Status.Builder().addTemplate(text).build())
                            getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification(text).build())
                        }
                    }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun statusText(state: WatchState?): String = when {
        state == null || !state.active -> "Workout"
        state.paused -> "Paused"
        state.allDone -> "All done"
        else -> "${state.exerciseName} · ${state.step}"
    }

    private fun notification(text: String): NotificationCompat.Builder {
        val channel = NotificationChannel(CHANNEL_ID, "Workout", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_workout)
            .setContentTitle("Workout")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(Notification.CATEGORY_WORKOUT)
            .setContentIntent(openApp())
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val CHANNEL_ID = "workout"
        const val NOTIFICATION_ID = 7
    }
}
