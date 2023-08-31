package be.planty.android.alexa.audioplayer

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import android.text.TextUtils
import android.util.Log
import be.planty.android.alexa.interfaces.AvsItem
import be.planty.android.alexa.interfaces.audioplayer.AvsPlayContentItem
import be.planty.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem
import be.planty.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CompletableFuture.runAsync

/**
 * A class that abstracts the Android MediaPlayer and adds additional functionality to handle AvsItems
 * as well as properly handle multiple callbacks--be care not to leak Activities by not removing the callback
 */
class AlexaAudioPlayer
/**
 * Create our new AlexaAudioPlayer
 * @param context any context, we will get the application level to store locally
 */
private constructor(context: Context) {

    private var mMediaPlayer: MediaPlayer? = null
    private val mContext: Context
    var currentItem: AvsItem? = null
        private set
    private val mCallbacks = ArrayList<Callback>()

    /**
     * Return a reference to the MediaPlayer instance, if it does not exist,
     * then create it and configure it to our needs
     * @return Android native MediaPlayer
     */
    private val mediaPlayer: MediaPlayer?
        get() {
            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer()
                mMediaPlayer!!.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK)
                mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                mMediaPlayer!!.setOnCompletionListener(mCompletionListener)
                mMediaPlayer!!.setOnPreparedListener(mPreparedListener)
                mMediaPlayer!!.setOnErrorListener(mErrorListener)
            }
            return mMediaPlayer
        }

    /**
     * Check whether our MediaPlayer is currently playing
     * @return true playing, false not
     */
    val isPlaying: Boolean
        get() = mediaPlayer!!.isPlaying

    /**
     * Pass our MediaPlayer completion state to all the Callbacks, handle it at the top level
     */
    private val mCompletionListener = MediaPlayer.OnCompletionListener {
        for (callback in mCallbacks) {
            callback.playerProgress(currentItem, 1, 1f)
            callback.itemComplete(currentItem)
        }
    }

    /**
     * Pass our MediaPlayer prepared state to all the Callbacks, handle it at the top level
     */
    private val mPreparedListener = MediaPlayer.OnPreparedListener {
        for (callback in mCallbacks) {
            callback.playerPrepared(currentItem)
            callback.playerProgress(currentItem, mMediaPlayer!!.currentPosition.toLong(), 0f)
        }
        mMediaPlayer!!.start()
        runAsync {
            try {
                while (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    val pos = mediaPlayer!!.currentPosition
                    val percent = pos.toFloat() / mediaPlayer!!.duration.toFloat()
                    postProgress(percent)
                    try {
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Pass our MediaPlayer error state to all the Callbacks, handle it at the top level
     */
    private val mErrorListener = MediaPlayer.OnErrorListener { mp, what, extra ->
        for (callback in mCallbacks) {
            val response = callback.playerError(currentItem, what, extra)
            if (response) {
                return@OnErrorListener response
            }
        }
        false
    }

    init {
        mContext = context.applicationContext
    }

    /**
     * Add a callback to our AlexaAudioPlayer, this is added to our list of callbacks
     * @param callback Callback that listens to changes of player state
     */
    fun addCallback(callback: Callback) {
        synchronized(mCallbacks) {
            if (!mCallbacks.contains(callback)) {
                mCallbacks.add(callback)
            }
        }
    }

    /**
     * Remove a callback from our AlexaAudioPlayer, this is removed from our list of callbacks
     * @param callback Callback that listens to changes of player state
     */
    fun removeCallback(callback: Callback) {
        synchronized(mCallbacks) {
            mCallbacks.remove(callback)
        }
    }

    /**
     * A helper function to play an AvsPlayContentItem, this is passed to play() and handled accordingly,
     * @param item a speak type item
     */
    fun playItem(item: AvsPlayContentItem) {
        play(item)
    }

    /**
     * A helper function to play an AvsSpeakItem, this is passed to play() and handled accordingly,
     * @param item a speak type item
     */
    fun playItem(item: AvsSpeakItem) {
        play(item)
    }

    /**
     * A helper function to play an AvsPlayRemoteItem, this is passed to play() and handled accordingly,
     * @param item a play type item, usually a url
     */
    fun playItem(item: AvsPlayRemoteItem) {
        play(item)
    }

    /**
     * Request our MediaPlayer to play an item, if it's an AvsPlayRemoteItem (url, usually), we set that url as our data source for the MediaPlayer
     * if it's an AvsSpeakItem, then we write the raw audio to a file and then read it back using the MediaPlayer
     * @param item
     */
    private fun play(item: AvsItem) {
        if (isPlaying) {
            Log.w(TAG, "Already playing an item, did you mean to play another?")
        }
        currentItem = item
        if (mediaPlayer!!.isPlaying) {
            //if we're playing, stop playing before we continue
            mediaPlayer!!.stop()
        }

        //reset our player
        mediaPlayer!!.reset()

        if (!TextUtils.isEmpty(currentItem!!.token) && currentItem!!.token.contains("PausePrompt")) {
            //a gross work around for a broke pause mp3 coming from Amazon, play the local mp3
            try {
                val afd = mContext.assets.openFd("shhh.mp3")
                mediaPlayer!!.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            } catch (e: IOException) {
                e.printStackTrace()
                //bubble up our error
                bubbleUpError(e)
            }

        } else if (currentItem is AvsPlayRemoteItem) {
            //cast our item for easy access
            val playItem = item as AvsPlayRemoteItem
            try {
                //set stream
                mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                //play new url
                mediaPlayer!!.setDataSource(playItem.url)
            } catch (e: IOException) {
                e.printStackTrace()
                //bubble up our error
                bubbleUpError(e)
            }

        } else if (currentItem is AvsPlayContentItem) {
            //cast our item for easy access
            val playItem = item as AvsPlayContentItem
            try {
                //set stream
                mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                //play new url
                mediaPlayer!!.setDataSource(mContext, playItem.uri)
            } catch (e: IOException) {
                e.printStackTrace()
                //bubble up our error
                bubbleUpError(e)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                //bubble up our error
                bubbleUpError(e)
            }

        } else if (currentItem is AvsSpeakItem) {
            //cast our item for easy access
            val playItem = item as AvsSpeakItem
            //write out our raw audio data to a file
            val path = File(mContext.cacheDir, System.currentTimeMillis().toString() + ".mp3")
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(path)
                fos.write(playItem.audio)
                fos.close()
                //play our newly-written file
                mediaPlayer!!.setDataSource(path.path)
            } catch (e: IOException) {
                e.printStackTrace()
                //bubble up our error
                bubbleUpError(e)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                bubbleUpError(e)
            }

        }
        //prepare our player, this will start once prepared because of mPreparedListener
        try {
            mediaPlayer!!.prepareAsync()
        } catch (e: IllegalStateException) {
            bubbleUpError(e)
        }

    }

    /**
     * A helper function to pause the MediaPlayer
     */
    fun pause() {
        mediaPlayer!!.pause()
    }

    /**
     * A helper function to play the MediaPlayer
     */
    fun play() {
        mediaPlayer!!.start()
    }

    /**
     * A helper function to stop the MediaPlayer
     */
    fun stop() {
        mediaPlayer!!.stop()
    }

    /**
     * A helper function to release the media player and remove it from memory
     */
    fun release() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer!!.isPlaying) {
                mMediaPlayer!!.stop()
            }
            mMediaPlayer!!.reset()
            mMediaPlayer!!.release()
        }
        mMediaPlayer = null
    }

    fun duck(value: Float) {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.setVolume(value, value)
        }
    }

    fun unDuck() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.setVolume(1f, 1f)
        }
    }

    /**
     * If our callback is not null, post our player progress back to the controlling
     * application so we can do "almost done" type of calls
     */
    private fun postProgress(percent: Float) {
        synchronized(mCallbacks) {
            for (callback in mCallbacks) {
                if (mMediaPlayer != null && callback != null) {
                    callback.playerProgress(currentItem, mMediaPlayer!!.currentPosition.toLong(), percent)
                }
            }
        }
    }

    /**
     * A callback to keep track of the state of the MediaPlayer and various AvsItem states
     */
    interface Callback {
        fun playerPrepared(pendingItem: AvsItem?)
        fun playerProgress(currentItem: AvsItem?, offsetInMilliseconds: Long, percent: Float)
        fun itemComplete(completedItem: AvsItem?)
        fun playerError(item: AvsItem?, what: Int, extra: Int): Boolean
        fun dataError(item: AvsItem?, e: Exception)
    }

    /**
     * Pass our Exception to all the Callbacks, handle it at the top level
     * @param e the thrown exception
     */
    private fun bubbleUpError(e: Exception) {
        for (callback in mCallbacks) {
            callback.dataError(currentItem, e)
        }
    }

    companion object {

        val TAG = "AlexaAudioPlayer"

        private var mInstance: AlexaAudioPlayer? = null

        /**
         * Get a reference to the AlexaAudioPlayer instance, if it's null, we will create a new one
         * using the supplied context.
         * @param context any context, we will get the application level to store locally
         * @return our instance of the AlexaAudioPlayer
         */
        fun getInstance(context: Context): AlexaAudioPlayer {
            if (mInstance == null) {
                mInstance = AlexaAudioPlayer(context)
                trimCache(context)
            }
            return mInstance!!
        }

        private fun trimCache(context: Context) {
            try {
                val dir = context.cacheDir
                if (dir != null && dir.isDirectory) {
                    deleteDir(dir)
                }
            } catch (e: Exception) {
                // TODO: handle exception
            }

        }

        private fun deleteDir(dir: File?): Boolean {
            if (dir != null && dir.isDirectory) {
                val children = dir.list()
                for (i in children.indices) {
                    val success = deleteDir(File(dir, children[i]))
                    if (!success) {
                        return false
                    }
                }
            }
            return dir?.delete() ?: false
        }
    }


}
