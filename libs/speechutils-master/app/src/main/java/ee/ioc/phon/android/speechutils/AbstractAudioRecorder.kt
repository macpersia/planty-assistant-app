/*
 * Copyright 2011-2016, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.speechutils

import android.media.AudioFormat

import ee.ioc.phon.android.speechutils.utils.AudioUtils

abstract class AbstractAudioRecorder protected constructor(audioSource: Int, protected val sampleRate: Int) : AudioRecorder {

    private var mRecorder: SpeechRecord? = null

    private var mAvgEnergy = 0.0
    private val mOneSec: Int

    // Recorder state
    /**
     * @return recorder state
     */
    var state: State? = null
        protected set

    // The complete space into which the recording in written.
    // Its maximum length is about:
    // 2 (bytes) * 1 (channels) * 30 (max rec time in seconds) * 44100 (times per second) = 2 646 000 bytes
    // but typically is:
    // 2 (bytes) * 1 (channels) * 20 (max rec time in seconds) * 16000 (times per second) = 640 000 bytes
    private val mRecording: ByteArray

    // TODO: use: mRecording.length instead
    var length = 0
        private set

    // The number of bytes the client has already consumed
    protected var consumedLength = 0

    // Buffer for output
    private var mBuffer: ByteArray? = null

    protected val bufferSize: Int
        get() {
            var minBufferSizeInBytes = SpeechRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, RESOLUTION)
            if (minBufferSizeInBytes == SpeechRecord.ERROR_BAD_VALUE) {
                throw IllegalArgumentException("SpeechRecord.getMinBufferSize: parameters not supported by hardware")
            } else if (minBufferSizeInBytes == SpeechRecord.ERROR) {
                Log.e("SpeechRecord.getMinBufferSize: unable to query hardware for output properties")
                minBufferSizeInBytes = sampleRate * (120 / 1000) * RESOLUTION_IN_BYTES * CHANNELS
            }
            val bufferSize = BUFFER_SIZE_MUTLIPLIER * minBufferSizeInBytes
            Log.i("SpeechRecord buffer size: $bufferSize, min size = $minBufferSizeInBytes")
            return bufferSize
        }


    /**
     * @return bytes that have been recorded since the beginning
     */
    val completeRecording: ByteArray
        get() = getCurrentRecording(0)


    /**
     * @return bytes that have been recorded since the beginning, with wav-header
     */
    val completeRecordingAsWav: ByteArray
        get() = getRecordingAsWav(completeRecording, sampleRate)


    /**
     * @return `true` iff a speech-ending pause has occurred at the end of the recorded data
     */
    val isPausing: Boolean
        get() {
            val pauseScore = pauseScore
            Log.i("Pause score: " + pauseScore)
            return pauseScore > 7
        }


    /**
     * @return volume indicator that shows the average volume of the last read buffer
     */
    // TODO: why 10?
    val rmsdb: Float
        get() {
            val sumOfSquares = getRms(length, mBuffer!!.size)
            val rootMeanSquare = Math.sqrt(sumOfSquares / (mBuffer!!.size / 2))
            return if (rootMeanSquare > 1) {
                (10 * Math.log10(rootMeanSquare)) as Float
            } else 0f
        }


    /**
     *
     * In order to calculate if the user has stopped speaking we take the
     * data from the last second of the recording, map it to a number
     * and compare this number to the numbers obtained previously. We
     * return a confidence score (0-INF) of a longer pause having occurred in the
     * speech input.
     *
     *
     *
     * TODO: base the implementation on some well-known technique.
     *
     * @return positive value which the caller can use to determine if there is a pause
     */
    private val pauseScore: Double
        get() {
            val t2 = getRms(length, mOneSec)
            if (t2 == 0L) {
                return 0.0
            }
            val t = mAvgEnergy / t2
            mAvgEnergy = (2 * mAvgEnergy + t2) / 3
            return t
        }

    private val speechRecordState: Int
        get() = if (mRecorder == null) {
            SpeechRecord.STATE_UNINITIALIZED
        } else mRecorder!!.getState()

    init {
        // E.g. 1 second of 16kHz 16-bit mono audio takes 32000 bytes.
        mOneSec = RESOLUTION_IN_BYTES * CHANNELS * this.sampleRate
        // TODO: replace 35 with the max length of the recording
        mRecording = ByteArray(mOneSec * 35)
    }


    protected fun createRecorder(audioSource: Int, sampleRate: Int, bufferSize: Int) {
        mRecorder = SpeechRecord(audioSource, sampleRate, AudioFormat.CHANNEL_IN_MONO, RESOLUTION, bufferSize, false, false, false)
        if (speechRecordState != SpeechRecord.STATE_INITIALIZED) {
            throw IllegalStateException("SpeechRecord initialization failed")
        }
    }

    // TODO: remove
    protected fun createBuffer(framePeriod: Int) {
        mBuffer = ByteArray(framePeriod * RESOLUTION_IN_BYTES * CHANNELS)
    }

    /**
     * Returns the recorded bytes since the last call, and resets the recording.
     *
     * @return bytes that have been recorded since this method was last called
     */
    @Synchronized
    fun consumeRecordingAndTruncate(): ByteArray {
        val len = consumedLength
        val bytes = getCurrentRecording(len)
        setRecordedLength(0)
        consumedLength = 0
        return bytes
    }


    /**
     * Checking of the read status.
     * The total recording array has been pre-allocated (e.g. for 35 seconds of audio).
     * If it gets full (status == -5) then the recording is stopped.
     */
    protected fun getStatus(numOfBytes: Int, len: Int): Int {
        Log.i("Read bytes: request/actual: $len/$numOfBytes")
        if (numOfBytes < 0) {
            Log.e("AudioRecord error: " + numOfBytes)
            return numOfBytes
        }
        if (numOfBytes > len) {
            Log.e("Read more bytes than is buffer length:$numOfBytes: $len")
            return -100
        } else if (numOfBytes == 0) {
            Log.e("Read zero bytes")
            return -200
        } else if (mRecording.size < length + numOfBytes) {
            Log.e("Recorder buffer overflow: " + length)
            return -300
        }
        return 0
    }

    /**
     * Copy data from the given recorder into the given buffer, and append to the complete recording.
     * public int read (byte[] audioData, int offsetInBytes, int sizeInBytes)
     */
    protected fun read(recorder: SpeechRecord, buffer: ByteArray?): Int {
        val len = buffer!!.size
        val numOfBytes = recorder.read(buffer, 0, len)
        val status = getStatus(numOfBytes, len)
        if (status == 0) {
            // arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
            // numOfBytes <= len, typically == len, but at the end of the recording can be < len.
            System.arraycopy(buffer, 0, mRecording, length, numOfBytes)
            length += len
        }
        return status
    }

    /**
     * @return bytes that have been recorded since this method was last called
     */
    @Synchronized
    fun consumeRecording(): ByteArray {
        val bytes = getCurrentRecording(consumedLength)
        consumedLength = length
        return bytes
    }

    protected fun getCurrentRecording(startPos: Int): ByteArray {
        val len = length - startPos
        val bytes = ByteArray(len)
        System.arraycopy(mRecording, startPos, bytes, 0, len)
        Log.i("Copied from: " + startPos + ": " + bytes.size + " bytes")
        return bytes
    }

    protected fun setRecordedLength(len: Int) {
        length = len
    }


    /**
     *
     * Stops the recording (if needed) and releases the resources.
     * The object can no longer be used and the reference should be
     * set to null after a call to release().
     */
    @Synchronized
    fun release() {
        if (mRecorder != null) {
            if (mRecorder!!.getRecordingState() === SpeechRecord.RECORDSTATE_RECORDING) {
                stop()
            }
            mRecorder!!.release()
            mRecorder = null
        }
    }

    /**
     *
     * Starts the recording, and sets the state to RECORDING.
     */
    fun start() {
        if (speechRecordState == SpeechRecord.STATE_INITIALIZED) {
            mRecorder!!.startRecording()
            if (mRecorder!!.getRecordingState() === SpeechRecord.RECORDSTATE_RECORDING) {
                state = State.RECORDING
                object : Thread() {
                    fun run() {
                        recorderLoop(mRecorder)
                    }
                }.start()
            } else {
                handleError("startRecording() failed")
            }
        } else {
            handleError("start() called on illegal state")
        }
    }


    /**
     *
     * Stops the recording, and sets the state to STOPPED.
     * If stopping fails then sets the state to ERROR.
     */
    fun stop() {
        // We check the underlying SpeechRecord state trying to avoid IllegalStateException.
        // If it still occurs then we catch it.
        if (speechRecordState == SpeechRecord.STATE_INITIALIZED && mRecorder!!.getRecordingState() === SpeechRecord.RECORDSTATE_RECORDING) {
            try {
                mRecorder!!.stop()
                state = State.STOPPED
            } catch (e: IllegalStateException) {
                handleError("native stop() called in illegal state: " + e.getMessage())
            }

        } else {
            handleError("stop() called in illegal state")
        }
    }

    protected fun recorderLoop(recorder: SpeechRecord?) {
        while (recorder!!.getRecordingState() === SpeechRecord.RECORDSTATE_RECORDING) {
            val status = read(recorder, mBuffer)
            if (status < 0) {
                handleError("status = " + status)
                break
            }
        }
    }


    private fun getRms(end: Int, span: Int): Long {
        var begin = end - span
        if (begin < 0) {
            begin = 0
        }
        // make sure begin is even
        if (0 != begin % 2) {
            begin++
        }

        var sum: Long = 0
        var i = begin
        while (i < end) {
            val curSample = getShort(mRecording[i], mRecording[i + 1])
            sum += (curSample * curSample).toLong()
            i += 2
        }
        return sum
    }


    protected fun handleError(msg: String) {
        release()
        state = State.ERROR
        Log.e(msg)
    }

    companion object {

        private val RESOLUTION = AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE_MUTLIPLIER = 4 // was: 2


        fun getRecordingAsWav(pcm: ByteArray, sampleRate: Int): ByteArray {
            return AudioUtils.getRecordingAsWav(pcm, sampleRate, RESOLUTION_IN_BYTES, CHANNELS)
        }


        /*
     * Converts two bytes to a short (assuming little endian).
     * TODO: We don't need the whole short, just take the 2nd byte (the more significant one)
     * TODO: Most Android devices are little endian?
     */
        private fun getShort(argB1: Byte, argB2: Byte): Short {
            //if (ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)) {
            //    return (short) ((argB1 << 8) | argB2);
            //}
            return (argB1 or (argB2 shl 8)).toShort()
        }
    }
}