package com.gingercake.nsn.framework

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gingercake.nsn.R
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.auth.AuthActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NSNFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        try {
            FirebaseFirestore
                .getInstance()
                .collection(Constants.USERS_COLLECTION)
                .document(SessionManager.currentUser.uid)
                .update("fcmTokens", FieldValue.arrayUnion(token))
                .addOnFailureListener {
                    Log.w("NSNFirebaseMessagingService", "onUpdatingFCMToken: failed", it)
                }
                .addOnSuccessListener {
                    Log.d("NSNFirebaseMessagingService", "onUpdatingFCMToken: Successful")
                }
        } catch (e: Exception) {
            Log.w("NSNFirebaseMessagingService", "onNewToken: Failed", e)
        }
    }

    override fun onMessageReceived(remoteMEssage: RemoteMessage) {
        super.onMessageReceived(remoteMEssage)
        remoteMEssage.notification?.body?.let { body ->
            sendNotification(body, remoteMEssage.data)
        }
    }

    private fun sendNotification(messageBody: String, data: MutableMap<String, String>) {
        val intent = Intent(this, AuthActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (data.isNotEmpty()) {
            data.forEach { kv ->
                intent.putExtra(kv.key, kv.value)
            }
        }
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_baseline_account_circle_24)
            .setContentTitle(getString(R.string.fcm_message))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

}