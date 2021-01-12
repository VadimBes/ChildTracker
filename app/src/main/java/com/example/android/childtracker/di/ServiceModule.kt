package com.example.android.childtracker.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.example.android.childtracker.R
import com.example.android.childtracker.ui.ParentActivity
import com.example.android.childtracker.ui.SplashActivity
import com.example.android.childtracker.utils.Constants
import com.example.android.childtracker.utils.Extension.toBitmap
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    @ServiceScoped
    @Provides
    fun provideSplashActivityPendingIntent(
        @ApplicationContext app:Context
    )= PendingIntent.getActivity(
        app,
        0,
        Intent(app, SplashActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    @ServiceScoped
    @Provides
    fun provideBaseNotificationBuilder(
        @ApplicationContext app:Context,
        pendingIntent: PendingIntent
    ) = NotificationCompat.Builder(app,
        Constants.NOTIFICATION_CHANNEL_ID
    )
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_baseline_child_care_24)
        .setLargeIcon(app.getDrawable(R.drawable.ic_people_family)?.toBitmap())
        .setContentTitle("Child Tracker")
        .setContentText("Ваш ребенок находиться в разрешенной зоне")
        .setContentIntent(pendingIntent)
        .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
}