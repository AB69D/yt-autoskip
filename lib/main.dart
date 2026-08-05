import 'package:flutter/foundation.dart'
    show TargetPlatform, defaultTargetPlatform;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SkipWise',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF4F46E5)),
        useMaterial3: true,
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with WidgetsBindingObserver {
  static const _channel = MethodChannel(
    'com.devconnectx.skipwise/accessibility',
  );

  // The ad-skipper is an Android Accessibility Service that acts on the YouTube/Facebook
  // Android apps — there's no equivalent to hook into on desktop, so those platforms get
  // an explanatory screen instead of calling into the (Android-only) platform channel.
  // Uses defaultTargetPlatform (not dart:io Platform) so tests can fake the platform via
  // debugDefaultTargetPlatformOverride.
  bool get _isAndroid => defaultTargetPlatform == TargetPlatform.android;

  bool _serviceEnabled = false;
  bool _batteryUnrestricted = false;
  int _skipCount = 0;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    if (_isAndroid) {
      _refresh();
    } else {
      _loading = false;
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Re-check status whenever the user comes back from a system settings screen.
    if (state == AppLifecycleState.resumed && _isAndroid) {
      _refresh();
    }
  }

  Future<void> _refresh() async {
    try {
      final enabled =
          await _channel.invokeMethod<bool>('isServiceEnabled') ?? false;
      final battery =
          await _channel.invokeMethod<bool>('isIgnoringBatteryOptimizations') ??
          false;
      final count = await _channel.invokeMethod<int>('getSkipCount') ?? 0;
      if (!mounted) return;
      setState(() {
        _serviceEnabled = enabled;
        _batteryUnrestricted = battery;
        _skipCount = count;
        _loading = false;
      });
    } on PlatformException {
      if (!mounted) return;
      setState(() => _loading = false);
    }
  }

  Future<void> _openAccessibilitySettings() =>
      _channel.invokeMethod('openAccessibilitySettings');

  Future<void> _requestBatteryExemption() =>
      _channel.invokeMethod('requestIgnoreBatteryOptimizations');

  Future<void> _openAutostartSettings() =>
      _channel.invokeMethod('openAutostartSettings');

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('SkipWise')),
      body: !_isAndroid
          ? const _DesktopNoticeView()
          : _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _refresh,
              child: ListView(
                padding: const EdgeInsets.all(20),
                children: [
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(20),
                      child: Column(
                        children: [
                          Icon(
                            _serviceEnabled
                                ? Icons.check_circle
                                : Icons.error_outline,
                            color: _serviceEnabled ? Colors.green : Colors.red,
                            size: 56,
                          ),
                          const SizedBox(height: 12),
                          Text(
                            _serviceEnabled
                                ? 'Accessibility service: ON'
                                : 'Accessibility service: OFF',
                            textAlign: TextAlign.center,
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                          const SizedBox(height: 8),
                          Text(
                            'Ads skipped so far: $_skipCount',
                            style: Theme.of(context).textTheme.bodyLarge,
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    'Setup checklist',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const Text(
                    'MIUI kills background apps aggressively by default — these three '
                    'steps stop it from silently switching the skipper off.',
                  ),
                  const SizedBox(height: 8),
                  _ChecklistTile(
                    done: _serviceEnabled,
                    title: 'Accessibility permission',
                    subtitle:
                        'Required so the app can read the screen and tap Skip.',
                    buttonLabel: 'Open Accessibility Settings',
                    onPressed: _openAccessibilitySettings,
                  ),
                  _ChecklistTile(
                    done: _batteryUnrestricted,
                    title: 'Unrestricted battery use',
                    subtitle:
                        'Stops MIUI from freezing the app in the background.',
                    buttonLabel: 'Allow unrestricted battery use',
                    onPressed: _requestBatteryExemption,
                  ),
                  _ChecklistTile(
                    done:
                        null, // no public API to check this — always actionable
                    title: 'Xiaomi Autostart permission',
                    subtitle:
                        'Lets the app relaunch itself if MIUI ever kills it.',
                    buttonLabel: 'Open Autostart settings',
                    onPressed: _openAutostartSettings,
                  ),
                  _ChecklistTile(
                    done: null,
                    title: 'Lock the app in Recents',
                    subtitle:
                        'Open Recents (square button), find SkipWise, tap the '
                        'lock icon on its card. Swiping it away without locking lets '
                        'MIUI kill it.',
                    buttonLabel: null,
                    onPressed: null,
                  ),
                  const SizedBox(height: 12),
                  Center(
                    child: TextButton(
                      onPressed: _refresh,
                      child: const Text('Refresh status'),
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}

class _DesktopNoticeView extends StatelessWidget {
  const _DesktopNoticeView();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 480),
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.desktop_windows_outlined,
                size: 56,
                color: Colors.grey.shade600,
              ),
              const SizedBox(height: 16),
              Text(
                'Ad-skipping is Android-only',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 12),
              const Text(
                'SkipWise works by using Android\'s Accessibility Service to tap "Skip Ad" '
                'inside the YouTube and Facebook Android apps. Windows, Linux, and macOS '
                'don\'t run those apps, so there\'s nothing for this feature to act on here.\n\n'
                'This desktop build exists so the project can be built and inspected on '
                'every platform, but the automatic ad-skipping itself only runs on an '
                'Android phone or tablet with the app installed.',
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ChecklistTile extends StatelessWidget {
  const _ChecklistTile({
    required this.done,
    required this.title,
    required this.subtitle,
    required this.buttonLabel,
    required this.onPressed,
  });

  final bool? done; // null = status can't be checked programmatically
  final String title;
  final String subtitle;
  final String? buttonLabel;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(top: 8),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(
              done == true
                  ? Icons.check_circle
                  : done == false
                  ? Icons.radio_button_unchecked
                  : Icons.info_outline,
              color: done == true ? Colors.green : Colors.grey,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: Theme.of(context).textTheme.titleSmall),
                  const SizedBox(height: 4),
                  Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
                  if (buttonLabel != null) ...[
                    const SizedBox(height: 8),
                    OutlinedButton(
                      onPressed: onPressed,
                      child: Text(buttonLabel!),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
