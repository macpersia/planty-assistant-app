package ee.ioc.phon.android.speechutils.utils

import android.annotation.TargetApi
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.text.TextUtils

import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedList

import ee.ioc.phon.android.speechutils.Log
import ee.ioc.phon.android.speechutils.MediaFormatFactory

object AudioUtils {

    fun getRecordingAsWav(pcm: ByteArray, sampleRate: Int, resolutionInBytes: Short, channels: Short): ByteArray {
        val headerLen = 44
        val byteRate = sampleRate * resolutionInBytes // sampleRate*(16/8)*1 ???
        val totalAudioLen = pcm.size
        val totalDataLen = totalAudioLen + headerLen

        val header = ByteArray(headerLen)

        header[0] = 'R'.toByte()  // RIFF/WAVE header
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte()  // 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1  // format = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (2 * 16 / 8).toByte()  // block align
        header[33] = 0
        header[34] = (8 * resolutionInBytes).toByte()  // bits per sample
        header[35] = 0
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()

        val wav = ByteArray(header.size + pcm.size)
        System.arraycopy(header, 0, wav, 0, header.size)
        System.arraycopy(pcm, 0, wav, header.size, pcm.size)
        return wav
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun getAvailableEncoders(sampleRate: Int): List<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val format = MediaFormatFactory.createMediaFormat(MediaFormatFactory.Type.FLAC, sampleRate)
            val mcl = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val encoderAsStr = mcl.findEncoderForFormat(format)
            val encoders = ArrayList<String>()
            for (info in mcl.codecInfos) {
                if (info.isEncoder) {
                    if (info.name == encoderAsStr) {
                        encoders.add("*** " + info.name + ": " + TextUtils.join(", ", info.supportedTypes))
                    } else {
                        encoders.add(info.name + ": " + TextUtils.join(", ", info.supportedTypes))
                    }
                }
            }
            return encoders
        }
        return emptyList<String>()
    }

    /**
     * Maps the given mime type to a list of names of suitable codecs.
     * Only OMX-codecs are considered.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun getEncoderNamesForType(mime: String): List<String> {
        val names = LinkedList<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val n = MediaCodecList.getCodecCount()
            for (i in 0 until n) {
                val info = MediaCodecList.getCodecInfoAt(i)
                if (!info.isEncoder) {
                    continue
                }
                if (!info.name.startsWith("OMX.")) {
                    // Unfortunately for legacy reasons, "AACEncoder", a
                    // non OMX component had to be in this list for the video
                    // editor code to work... but it cannot actually be instantiated
                    // using MediaCodec.
                    Log.i("skipping '" + info.name + "'.")
                    continue
                }
                val supportedTypes = info.supportedTypes
                for (j in supportedTypes.indices) {
                    if (supportedTypes[j].equals(mime, ignoreCase = true)) {
                        names.push(info.name)
                        break
                    }
                }
            }
        }
        // Return an empty list if API is too old
        // TODO: maybe return null or throw exception
        return names
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun createCodec(componentName: String, format: MediaFormat): MediaCodec? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                val codec = MediaCodec.createByCodecName(componentName)
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                return codec
            } catch (e: IllegalStateException) {
                Log.e("codec '$componentName' failed configuration.")
            } catch (e: IOException) {
                Log.e("codec '$componentName' failed configuration.")
            }

        }
        return null
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun showMetrics(format: MediaFormat, numBytesSubmitted: Int, numBytesDequeued: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Log.i("queued a total of $numBytesSubmitted bytes, dequeued $numBytesDequeued bytes.")
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val inBitrate = sampleRate * channelCount * 16  // bit/sec
            val outBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE)
            val desiredRatio = outBitrate.toFloat() / inBitrate.toFloat()
            val actualRatio = numBytesDequeued.toFloat() / numBytesSubmitted.toFloat()
            Log.i("desiredRatio = $desiredRatio, actualRatio = $actualRatio")
        }
    }

    fun concatenateBuffers(buffers: List<ByteArray>): ByteArray {
        val buffersConcatenated: ByteArray
        var sum = 0
        for (ba in buffers) {
            sum = sum + ba.size
        }
        buffersConcatenated = ByteArray(sum)
        var pos = 0
        for (ba in buffers) {
            System.arraycopy(ba, 0, buffersConcatenated, pos, ba.size)
            pos = pos + ba.size
        }
        return buffersConcatenated
    }

    /**
     * Just for testing...
     */
    fun showSomeBytes(tag: String, bytes: ByteArray) {
        Log.i("enc: " + tag + ": length: " + bytes.size)
        var str = ""
        val len = bytes.size
        if (len > 0) {
            var i = 0
            while (i < len && i < 5) {
                str += Integer.toHexString(bytes[i].toInt()) + " "
                i++
            }
            Log.i("enc: $tag: hex: $str")
        }
    }
}