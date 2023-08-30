/*
 * Copyright 2015-2016, Institute of Cybernetics at Tallinn University of Technology
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

package be.planty.android.speechutils

import android.annotation.TargetApi
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build

import java.nio.ByteBuffer

import be.planty.android.speechutils.utils.AudioUtils

/**
 * Based on https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/EncoderTest.java
 * Requires Android v4.1 / API 16 / JELLY_BEAN
 * TODO: support other formats than FLAC
 */
class EncodedAudioRecorder @JvmOverloads constructor(audioSource: Int = AudioRecorder.DEFAULT_AUDIO_SOURCE, sampleRate: Int = AudioRecorder.DEFAULT_SAMPLE_RATE) : AbstractAudioRecorder(audioSource, sampleRate) {

    // TODO: Use queue of byte[]
    private val mRecordingEnc: ByteArray
    private var recordedEncLength = 0
    private var consumedEncLength = 0

    private var mNumBytesSubmitted = 0
    private var mNumBytesDequeued = 0

    /**
     * TODO: the MIME should be configurable as the server might not support all formats
     * (returning "Your GStreamer installation is missing a plug-in.")
     * TODO: according to the server docs, for encoded data we do not need to specify the content type
     * such as "audio/x-flac", but it did not work without (nor with "audio/flac").
     */
    override val wsArgs: String
        get() = "?content-type=audio/x-flac"

    init {
        try {
            val bufferSize = bufferSize
            createRecorder(audioSource, sampleRate, bufferSize)
            val framePeriod = bufferSize / (2 * AudioRecorder.RESOLUTION_IN_BYTES.toInt() * AudioRecorder.CHANNELS.toInt())
            createBuffer(framePeriod)
            state = AudioRecorder.State.READY
        } catch (e: Exception) {
            handleError(e.message ?: "Unknown error occurred while initializing recording")
        }

        // TODO: replace 35 with the max length of the recording
        mRecordingEnc = ByteArray(AudioRecorder.RESOLUTION_IN_BYTES.toInt() * AudioRecorder.CHANNELS.toInt() * sampleRate * 35) // 35 sec raw
    }

//    @JvmOverloads
//    constructor(sampleRate: Int) : this(AudioRecorder.DEFAULT_AUDIO_SOURCE, sampleRate) {}

    @Synchronized
    fun consumeRecordingEncAndTruncate(): ByteArray {
        val len = consumedEncLength
        val bytes = getCurrentRecordingEnc(len)
        recordedEncLength = 0
        consumedEncLength = 0
        return bytes
    }

    /**
     * @return bytes that have been recorded and encoded since this method was last called
     */
    @Synchronized
    fun consumeRecordingEnc(): ByteArray {
        val bytes = getCurrentRecordingEnc(consumedEncLength)
        consumedEncLength = recordedEncLength
        return bytes
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun recorderLoop(speechRecord: SpeechRecord?) {
        mNumBytesSubmitted = 0
        mNumBytesDequeued = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val format = MediaFormatFactory.createMediaFormat(MediaFormatFactory.Type.FLAC, sampleRate)
            val componentNames = AudioUtils.getEncoderNamesForType(format!!.getString(MediaFormat.KEY_MIME)!!)
            for (componentName in componentNames) {
                Log.i("component/format: $componentName/$format")
                val codec = AudioUtils.createCodec(componentName, format)
                if (codec != null) {
                    recorderEncoderLoop(codec, speechRecord)
                    if (Log.DEBUG) {
                        AudioUtils.showMetrics(format, mNumBytesSubmitted, mNumBytesDequeued)
                    }
                    break // TODO: we use the first one that is suitable
                }
            }
        }
    }

    private fun addEncoded(buffer: ByteArray) {
        val len = buffer.size
        if (mRecordingEnc.size >= recordedEncLength + len) {
            System.arraycopy(buffer, 0, mRecordingEnc, recordedEncLength, len)
            recordedEncLength += len
        } else {
            handleError("RecorderEnc buffer overflow: " + recordedEncLength)
        }
    }

    private fun getCurrentRecordingEnc(startPos: Int): ByteArray {
        val len = recordedEncLength - startPos
        val bytes = ByteArray(len)
        System.arraycopy(mRecordingEnc, startPos, bytes, 0, len)
        Log.i("Copied from: " + startPos + ": " + bytes.size + " bytes")
        return bytes
    }

    /**
     * Copy audio from the recorder into the encoder.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun queueInputBuffer(codec: MediaCodec, inputBuffers: Array<ByteBuffer>, index: Int, speechRecord: SpeechRecord?): Int {
        if (speechRecord == null || speechRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            return -1
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val inputBuffer = inputBuffers[index]
            inputBuffer.clear()
            val size = inputBuffer.limit()
            val buffer = ByteArray(size)
            val status = read(speechRecord, buffer)
            if (status < 0) {
                handleError("status = " + status)
                return -1
            }
            inputBuffer.put(buffer)
            codec.queueInputBuffer(index, 0, size, 0, 0)
            return size
        }
        return -1
    }

    /**
     * Save the encoded (output) buffer into the complete encoded recording.
     * TODO: copy directly (without the intermediate byte array)
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun dequeueOutputBuffer(codec: MediaCodec, outputBuffers: Array<ByteBuffer>, index: Int, info: MediaCodec.BufferInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val buffer = outputBuffers[index]
            Log.i("size/remaining: " + info.size + "/" + buffer.remaining())
            if (info.size <= buffer.remaining()) {
                val bufferCopied = ByteArray(info.size)
                buffer.get(bufferCopied) // TODO: catch BufferUnderflow
                // TODO: do we need to clear?
                // on N5: always size == remaining(), clearing is not needed
                // on SGS2: remaining decreases until it becomes less than size, which results in BufferUnderflow
                // (but SGS2 records only zeros anyway)
                //buffer.clear();
                codec.releaseOutputBuffer(index, false)
                addEncoded(bufferCopied)
                if (Log.DEBUG) {
                    AudioUtils.showSomeBytes("out", bufferCopied)
                }
            } else {
                Log.e("size > remaining")
                codec.releaseOutputBuffer(index, false)
            }
        }
    }

    /**
     * Reads bytes from the given recorder and encodes them with the given encoder.
     * Uses the (deprecated) Synchronous Processing using Buffer Arrays.
     *
     *
     * Encoders (or codecs that generate compressed data) will create and return the codec specific
     * data before any valid output buffer in output buffers marked with the codec-config flag.
     * Buffers containing codec-specific-data have no meaningful timestamps.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun recorderEncoderLoop(codec: MediaCodec, speechRecord: SpeechRecord?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            codec.start()
            // Getting some buffers (e.g. 4 of each) to communicate with the codec
            val codecInputBuffers = codec.inputBuffers
            var codecOutputBuffers = codec.outputBuffers
            Log.i("input buffers " + codecInputBuffers.size + "; output buffers: " + codecOutputBuffers.size)
            var doneSubmittingInput = false
            var numRetriesDequeueOutputBuffer = 0
            var index: Int
            while (true) {
                if (!doneSubmittingInput) {
                    index = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT)
                    if (index >= 0) {
                        val size = queueInputBuffer(codec, codecInputBuffers, index, speechRecord)
                        if (size == -1) {
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            Log.i("enc: in: EOS")
                            doneSubmittingInput = true
                        } else {
                            Log.i("enc: in: " + size)
                            mNumBytesSubmitted += size
                        }
                    } else {
                        Log.i("enc: in: timeout, will try again")
                    }
                }
                val info = MediaCodec.BufferInfo()
                index = codec.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT)
                Log.i("enc: out: flags/index: " + info.flags + "/" + index)
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.i("enc: out: INFO_TRY_AGAIN_LATER: " + numRetriesDequeueOutputBuffer)
                    if (++numRetriesDequeueOutputBuffer > MAX_NUM_RETRIES_DEQUEUE_OUTPUT_BUFFER) {
                        break
                    }
                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val format = codec.outputFormat
                    Log.i("enc: out: INFO_OUTPUT_FORMAT_CHANGED: " + format.toString())
                } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    codecOutputBuffers = codec.outputBuffers
                    Log.i("enc: out: INFO_OUTPUT_BUFFERS_CHANGED")
                } else {
                    dequeueOutputBuffer(codec, codecOutputBuffers, index, info)
                    mNumBytesDequeued += info.size
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        Log.i("enc: out: EOS")
                        break
                    }
                }
            }
            codec.stop()
            codec.release()
        }
    }

    companion object {

        // Stop encoding if output buffer has not been available that many times.
        private val MAX_NUM_RETRIES_DEQUEUE_OUTPUT_BUFFER = 500

        // Time period to dequeue a buffer
        private val DEQUEUE_TIMEOUT: Long = 10000
    }
}