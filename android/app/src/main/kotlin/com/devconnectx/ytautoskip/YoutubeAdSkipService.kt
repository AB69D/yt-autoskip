package com.devconnectx.ytautoskip

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class YoutubeAdSkipService : AccessibilityService() {

    private var lastScanTime = 0L
    private lateinit var prefs: SharedPreferences

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        Log.i(TAG, "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < SCAN_THROTTLE_MS) return
        lastScanTime = now

        val root = rootInActiveWindow ?: return
        val pkg = (event?.packageName ?: root.packageName)?.toString() ?: return
        try {
            val target = when (pkg) {
                YOUTUBE_PKG -> findYoutubeSkipNode(root)
                FACEBOOK_PKG -> findFacebookSkipNode(root)
                else -> null
            } ?: return
            val maxClimb = if (pkg == FACEBOOK_PKG) 4 else 10
            val clickable = nearestClickableAncestor(target, maxClimb) ?: return
            if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                val count = prefs.getInt(KEY_COUNT, 0) + 1
                prefs.edit().putInt(KEY_COUNT, count).apply()
                Log.i(TAG, "Skip clicked in $pkg, total=$count")
            }
        } catch (e: Exception) {
            Log.w(TAG, "scan failed", e)
        }
    }

    // Depth-first walk of the current screen's accessibility tree (same idea as a UI dump),
    // looking for a resource-id or label that identifies YouTube's "Skip Ad" control.
    // Matched by substring rather than an exact id ("skip" not "skip_ad") because different
    // ad formats (in-player pre-roll vs. full-screen/interstitial) use different ids across
    // app versions, and some only carry the label in contentDescription, not text.
    private fun findYoutubeSkipNode(node: AccessibilityNodeInfo, depth: Int = 0): AccessibilityNodeInfo? {
        if (depth > 50) return null

        val viewId = node.viewIdResourceName ?: ""
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        if (viewId.contains("skip", ignoreCase = true) || isSkipLabel(text) || isSkipLabel(desc)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findYoutubeSkipNode(child, depth + 1)
            if (found != null) return found
        }
        return null
    }

    // Facebook has no known stable "Skip Ad" resource-id (unlike YouTube's descriptive ids),
    // and the feed packs many small tappable targets close together (Like/Share/post body).
    // A loose match here risks tapping something other than an ad control, so this only
    // matches an exact "Skip"/"Skip Ad" label and climbs a much shorter ancestor chain.
    private fun findFacebookSkipNode(node: AccessibilityNodeInfo, depth: Int = 0): AccessibilityNodeInfo? {
        if (depth > 50) return null

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        if (isExactSkipLabel(text) || isExactSkipLabel(desc)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFacebookSkipNode(child, depth + 1)
            if (found != null) return found
        }
        return null
    }

    private fun isSkipLabel(s: String): Boolean {
        if (s.isBlank()) return false
        val normalized = s.trim().lowercase()
        return normalized.startsWith("skip ad") || normalized.startsWith("skip ads") ||
            normalized.startsWith("skip video") || normalized.startsWith("skip this ad") ||
            normalized == "skip"
    }

    private fun isExactSkipLabel(s: String): Boolean {
        if (s.isBlank()) return false
        val normalized = s.trim().lowercase()
        return normalized == "skip" || normalized == "skip ad" || normalized == "skip ads"
    }

    private fun nearestClickableAncestor(node: AccessibilityNodeInfo, maxDepth: Int): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < maxDepth) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    override fun onInterrupt() {}

    companion object {
        private const val TAG = "YTAutoSkip"
        private const val SCAN_THROTTLE_MS = 250L
        private const val YOUTUBE_PKG = "com.google.android.youtube"
        private const val FACEBOOK_PKG = "com.facebook.katana"
        const val PREFS_NAME = "ytautoskip_prefs"
        const val KEY_COUNT = "skip_count"
    }
}
