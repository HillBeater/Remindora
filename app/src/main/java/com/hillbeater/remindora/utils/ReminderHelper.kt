package com.hillbeater.remindora.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderHelper(private val context: Context) {

    companion object {
        const val EXTRA_REMINDER_TITLE = "reminder_title"
        const val EXTRA_REMINDER_MESSAGE = "reminder_message"
        const val EXTRA_ITEM_ID = "item_id"
    }

    fun setReminder(itemId: Int, time: String, date: String, title: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val localDate = LocalDate.parse(date, dateFormatter)
        val localTime = LocalTime.parse(time, timeFormatter)
        val dateTime = LocalDateTime.of(localDate, localTime)

        val now = System.currentTimeMillis()
        val reminderTimeMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val reminderTimeMinus15Millis = dateTime.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Only set alarms if the time is in the future
        if (reminderTimeMinus15Millis > now) {
            setAlarm(
                itemId + 1000000,
                title,
                "Heads up! Only 15 minutes left for '$title'.",
                reminderTimeMinus15Millis
            )
        }

        if (reminderTimeMillis > now) {
            setAlarm(
                itemId,
                title,
                "It's time! Your reminder: '$title' has started.",
                reminderTimeMillis
            )
        }
    }

    private fun setAlarm(requestCode: Int, title: String, message: String, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_REMINDER_MESSAGE, message)
            putExtra(EXTRA_ITEM_ID, if (requestCode >= 1000000) requestCode - 1000000 else requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelReminder(itemId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val ids = listOf(itemId, itemId + 1000000)
        for (id in ids) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }
}
