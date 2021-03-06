/*
 * Copyright 2011-2015, Institute of Cybernetics at Tallinn University of Technology
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

/**
 *
 * Records raw audio using SpeechRecord and stores it into a byte array as
 *
 *  * signed
 *  * 16-bit
 *  * native endian
 *  * mono
 *  * 16kHz (recommended, but a different sample rate can be specified in the constructor)
 *
 *
 *
 *
 * For example, the corresponding `arecord` settings are
 *
 *
 * <pre>
 * arecord --file-type raw --format=S16_LE --channels 1 --rate 16000
 * arecord --file-type raw --format=S16_BE --channels 1 --rate 16000 (possibly)
</pre> *
 *
 *
 * TODO: maybe use: ByteArrayOutputStream
 *
 * @author Kaarel Kaljurand
 */
class RawAudioRecorder
/**
 *
 * Instantiates a new recorder and sets the state to INITIALIZING.
 * In case of errors, no exception is thrown, but the state is set to ERROR.
 *
 *
 *
 * Android docs say: 44100Hz is currently the only rate that is guaranteed to work on all devices,
 * but other rates such as 22050, 16000, and 11025 may work on some devices.
 *
 * @param audioSource Identifier of the audio source (e.g. microphone)
 * @param sampleRate  Sample rate (e.g. 16000)
 */
@JvmOverloads constructor(
        audioSource: Int = AudioRecorder.DEFAULT_AUDIO_SOURCE,
        sampleRate: Int = AudioRecorder.DEFAULT_SAMPLE_RATE)
    : AbstractAudioRecorder(audioSource, sampleRate) {

    override val wsArgs: String
        get() = "?content-type=audio/x-raw,+layout=(string)interleaved,+rate=(int)$sampleRate,+format=(string)S16LE,+channels=(int)1"

    init {
        try {
            val bufferSize = bufferSize
            val framePeriod = bufferSize / (2 * AudioRecorder.RESOLUTION_IN_BYTES.toInt() * AudioRecorder.CHANNELS.toInt())
            createRecorder(audioSource, sampleRate, bufferSize)
            createBuffer(framePeriod)
            state = AudioRecorder.State.READY
        } catch (e: Exception) {
            handleError(e?.message ?: "Unknown error occurred while initializing recorder")
        }

    }

//    @JvmOverloads
//    constructor(sampleRate: Int) : this(AudioRecorder.DEFAULT_AUDIO_SOURCE, sampleRate) {}
}
