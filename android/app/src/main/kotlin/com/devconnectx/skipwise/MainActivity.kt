package com.devconnectx.skipwise

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val channelName = "com.devconnectx.skipwise/accessibility"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName).setMethodCallHandler { call, result ->
            when (call.method) {
                "isServiceEnabled" -> result.success(isServiceEnabled())
                "openAccessibilitySettings" -> {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    result.success(null)
                }
                "getSkipCount" -> {
                    val prefs = getSharedPreferences(AdSkipAccessibilityService.PREFS_NAME, MODE_PRIVATE)
                    result.success(prefs.getInt(AdSkipAccessibilityService.KEY_COUNT, 0))
                }
                "isIgnoringBatteryOptimizations" -> result.success(isIgnoringBatteryOptimizations())
                "requestIgnoreBatteryOptimizations" -> {
                    requestIgnoreBatteryOptimizations()
                    result.success(null)
                }
                "openAutostartSettings" -> result.success(openXiaomiAutostartSettings())
                else -> result.notImplemented()
            }
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    @Suppress("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        if (isIgnoringBatteryOptimizations()) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    // Xiaomi/MIUI's "Autostart" screen has no public AOSP API. This targets the
    // widely-used undocumented activity that MIUI's own Security app exposes for it;
    // it simply does nothing (via the catch) on non-MIUI devices or if the ROM changes it.
    private fun openXiaomiAutostartSettings(): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Compares parsed ComponentNames rather than raw strings: MIUI (and some AOSP paths)
    // stores the shorthand ".AdSkipAccessibilityService" form here instead of the fully-qualified
    // class name, which a plain string-equals check misses — reporting OFF even when the
    // system's own Accessibility settings screen shows the service On.
    private fun isServiceEnabled(): Boolean {
        val target = ComponentName(this, AdSkipAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        for (s in splitter) {
            val component = ComponentName.unflattenFromString(s) ?: continue
            if (component == target) return true
        }
        return false
    }
}
