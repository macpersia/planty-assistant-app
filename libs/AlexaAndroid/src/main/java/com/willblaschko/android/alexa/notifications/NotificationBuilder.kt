package com.willblaschko.android.alexa.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.annotation.AnyRes
import android.support.annotation.DrawableRes
import android.support.v4.app.NotificationCompat

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList

/**
 * @author willb_000 on 12/29/2015.
 */
class NotificationBuilder {

    internal lateinit var mTitle: String
    internal lateinit var mDescription: String
    internal lateinit var mImage: String
    internal var mLargeImage: Bitmap? = null
    internal lateinit var mBackground: String
    internal lateinit var mIntent: PendingIntent
    internal var mSmallIcon: Int = 0
    internal var mPriority = NotificationCompat.PRIORITY_DEFAULT
    internal var mActions: MutableList<NotificationCompat.Action> = ArrayList()

    fun setTitle(title: String): NotificationBuilder {
        mTitle = title
        return this
    }

    fun setDescription(description: String): NotificationBuilder {
        mDescription = description
        return this
    }

    fun setImage(uri: String): NotificationBuilder {
        mImage = uri
        return this
    }

    fun setSmallIcon(@DrawableRes icon: Int): NotificationBuilder {
        mSmallIcon = icon
        return this
    }

    fun setLargeImage(bitmap: Bitmap): NotificationBuilder {
        mLargeImage = bitmap
        return this
    }

    fun setLargeImage(uri: String): NotificationBuilder {
        mLargeImage = getBitmapFromURL(uri)
        return this
    }


    fun setBackground(uri: String): NotificationBuilder {
        mBackground = uri
        return this
    }

    fun setIntent(intent: PendingIntent): NotificationBuilder {
        mIntent = intent
        return this
    }

    fun setPriority(priority: Int): NotificationBuilder {
        mPriority = priority
        return this
    }

    fun addAction(@DrawableRes drawable: Int, title: String, pendingIntent: PendingIntent): NotificationBuilder {
        mActions.add(NotificationCompat.Action(drawable, title, pendingIntent))
        return this
    }

    @Throws(IOException::class)
    fun build(context: Context): Notification {

        val builder = NotificationCompat.Builder(context)
                .setContentTitle(mTitle)
                .setContentText(mDescription)
                .setPriority(mPriority)
                .setLocalOnly(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setCategory(Notification.CATEGORY_RECOMMENDATION)
                .setLargeIcon(mLargeImage)
                .setSmallIcon(mSmallIcon)
                .setContentIntent(mIntent)

        for (action in mActions) {
            builder.addAction(action)
        }

        val notification: Notification
        if (isDirectToTV(context)) {
            notification = NotificationCompat.BigPictureStyle(builder).build()
        } else {
            notification = builder.build()
        }
        return notification
    }

    companion object {

        fun isDirectToTV(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) || context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
            } else false
        }

        fun getBitmapFromURL(src: String): Bitmap? {
            try {
                val url = URL(src)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                return BitmapFactory.decodeStream(input)
            } catch (e: IOException) {
                // Log exception
                return null
            }

        }

        /**
         * get uri to drawable or any other resource type if u wish
         * @param context - context
         * @param drawableId - drawable res id
         * @return - uri
         */
        fun getUriToDrawable(context: Context, @AnyRes drawableId: Int): String {
            return (ContentResolver.SCHEME_ANDROID_RESOURCE +
                    "://" + context.resources.getResourcePackageName(drawableId)
                    + '/' + context.resources.getResourceTypeName(drawableId)
                    + '/' + context.resources.getResourceEntryName(drawableId))
        }
    }

}
