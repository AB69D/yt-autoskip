# Strip logging calls from release builds: shrinks the APK slightly and removes log
# tags/messages (service name, package names, skip counters) that would otherwise make
# a decompiled build easier to read.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
