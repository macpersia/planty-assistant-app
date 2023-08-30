package be.planty.android.speechutils.view

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton

import java.util.ArrayList

import be.planty.android.speechutils.R

class MicButton : ImageButton {

    private var mDrawableMic: Drawable? = null
    private var mDrawableMicTranscribing: Drawable? = null
    private var mDrawableMicWaiting: Drawable? = null

    private var mVolumeLevels: MutableList<Drawable>? = null

    private var mAnimFadeInOutInf: Animation? = null

    private var mVolumeLevel = 0
    private var mMaxLevel: Int = 0

    enum class State {
        INIT, WAITING, RECORDING, LISTENING, TRANSCRIBING, ERROR
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        if (!isInEditMode) {
            init(context)
        }
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        if (!isInEditMode) {
            init(context)
        }
    }

    constructor(context: Context) : super(context) {
        if (!isInEditMode) {
            init(context)
        }
    }

    fun setState(state: State) {
        when (state) {
            State.INIT, State.ERROR -> {
                isEnabled = true
                clearAnimation()
                setBackgroundDrawable(mDrawableMic)
            }
            State.WAITING -> {
                isEnabled = false
                setBackgroundDrawable(mDrawableMicWaiting)
            }
            State.RECORDING -> isEnabled = true
            State.LISTENING -> {
                isEnabled = true
                setBackgroundDrawable(mVolumeLevels!![0])
            }
            State.TRANSCRIBING -> {
                isEnabled = true
                setBackgroundDrawable(mDrawableMicTranscribing)
                startAnimation(mAnimFadeInOutInf)
            }
            else -> {
            }
        }
    }


    fun setVolumeLevel(rmsdB: Float) {
        val index = ((rmsdB - DB_MIN) / (DB_MAX - DB_MIN) * mMaxLevel).toInt()
        val level = Math.min(Math.max(0, index), mMaxLevel)
        if (level != mVolumeLevel) {
            mVolumeLevel = level
            setBackgroundDrawable(mVolumeLevels!![level])
        }
    }

    private fun initAnimations(context: Context) {
        val res = resources
        mDrawableMic = res.getDrawable(R.drawable.button_mic)
        mDrawableMicTranscribing = res.getDrawable(R.drawable.button_mic_transcribing)
        mDrawableMicWaiting = res.getDrawable(R.drawable.button_mic_waiting)

        mVolumeLevels = ArrayList()
        mVolumeLevels!!.add(res.getDrawable(R.drawable.button_mic_recording_0))
        mVolumeLevels!!.add(res.getDrawable(R.drawable.button_mic_recording_1))
        mVolumeLevels!!.add(res.getDrawable(R.drawable.button_mic_recording_2))
        mVolumeLevels!!.add(res.getDrawable(R.drawable.button_mic_recording_3))
        mMaxLevel = mVolumeLevels!!.size - 1

        mAnimFadeInOutInf = AnimationUtils.loadAnimation(context, R.anim.fade_inout_inf)
    }

    private fun init(context: Context) {
        initAnimations(context)

        // Vibrate when the microphone key is pressed down
        setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // TODO: what is the diff between KEYBOARD_TAP and the other constants?
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            false
        }
    }

    companion object {

        // TODO: take these from some device specific configuration
        private val DB_MIN = 15.0f
        private val DB_MAX = 30.0f
    }
}