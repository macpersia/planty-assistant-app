package be.planty.assistant.app.utility

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale

/**
 * Utility functions to get the application signing key versus running the keytool in java
 */
object SigningKey {

    /**
     * Get the MD5 fingerprint required for application authentication on the server to set up security profiles
     * @param context any Context from the running application
     * @return the string equivalent of the MD5 fingerprint
     */
    fun getCertificateMD5Fingerprint(context: Context): String? {
        val pm = context.packageManager
        val packageName = context.packageName
        val flags = PackageManager.GET_SIGNATURES
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = pm.getPackageInfo(packageName, flags)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        val signatures = packageInfo!!.signatures
        val cert = signatures[0].toByteArray()
        val input = ByteArrayInputStream(cert)
        var cf: CertificateFactory? = null
        try {
            cf = CertificateFactory.getInstance("X509")
        } catch (e: CertificateException) {
            e.printStackTrace()
        }

        var c: X509Certificate? = null
        try {
            c = cf!!.generateCertificate(input) as X509Certificate
        } catch (e: CertificateException) {
            e.printStackTrace()
        }

        var hexString: String? = null
        try {
            val md = MessageDigest.getInstance("MD5")
            val publicKey = md.digest(c!!.encoded)
            hexString = byte2HexFormatted(publicKey)
        } catch (e1: NoSuchAlgorithmException) {
            e1.printStackTrace()
        } catch (e: CertificateEncodingException) {
            e.printStackTrace()
        }

        return hexString
    }

    /**
     * Convert the resulting byte array into a string for submission
     * @param arr the byte array supplied by getCertificateMD5Fingerprint
     * @return the string equivalent
     */
    fun byte2HexFormatted(arr: ByteArray): String {
        val str = StringBuilder(arr.size * 2)
        for (i in arr.indices) {
            var h = Integer.toHexString(arr[i].toInt())
            val l = h.length
            if (l == 1) h = "0" + h
            if (l > 2) h = h.substring(l - 2, l)
            str.append(h.uppercase(Locale.getDefault()))
            if (i < arr.size - 1) str.append(':')
        }
        return str.toString()
    }
}
