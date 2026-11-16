package com.lalatube.offline

import android.app.Application
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Menyimpan laporan crash terakhir ke penyimpanan aplikasi supaya penyebabnya bisa
 * dilihat dari menu Pengaturan tanpa perlu adb/logcat.
 */
class LalaTubeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { writeCrashLog(thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun writeCrashLog(thread: Thread, error: Throwable) {
        val stack = StringWriter().also { writer ->
            error.printStackTrace(PrintWriter(writer))
        }.toString()
        val version = runCatching {
            val info = packageManager.getPackageInfo(packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        }.getOrDefault("tidak diketahui")

        val text = buildString {
            appendLine("Waktu   : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("Perangkat: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Versi   : $version")
            appendLine("Thread  : ${thread.name}")
            appendLine()
            append(stack)
        }
        File(filesDir, "last-crash.txt").writeText(text)
    }
}
