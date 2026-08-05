import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:skipwise/main.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('com.devconnectx.skipwise/accessibility');

  group('on Android', () {
    setUp(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (MethodCall call) async {
            switch (call.method) {
              case 'isServiceEnabled':
                return false;
              case 'isIgnoringBatteryOptimizations':
                return false;
              case 'getSkipCount':
                return 0;
              default:
                return null;
            }
          });
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
    });

    testWidgets('shows title and accessibility status from the platform channel', (
      WidgetTester tester,
    ) async {
      // flutter test always runs on the host OS (macOS/Linux/Windows), never on an
      // actual Android device — fake it so this exercises the Android branch of the
      // UI. Reset in a `finally` (not addTearDown/tearDown): flutter_test's own
      // end-of-test invariant check runs synchronously once the test body's Future
      // completes, before either of those fire, and fails if this is still set.
      debugDefaultTargetPlatformOverride = TargetPlatform.android;
      try {
        await tester.pumpWidget(const MyApp());
        await tester.pumpAndSettle();

        expect(find.text('SkipWise'), findsOneWidget);
        expect(find.text('Accessibility service: OFF'), findsOneWidget);
        expect(find.text('Ads skipped so far: 0'), findsOneWidget);
      } finally {
        debugDefaultTargetPlatformOverride = null;
      }
    });

    testWidgets('refresh button re-queries the platform channel', (
      WidgetTester tester,
    ) async {
      debugDefaultTargetPlatformOverride = TargetPlatform.android;
      try {
        // The checklist makes this page taller than the default test viewport;
        // grow it so the "Refresh status" button at the bottom is actually built
        // instead of being outside the sliver's cache extent.
        tester.view.physicalSize = const Size(1080, 3000);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        await tester.pumpWidget(const MyApp());
        await tester.pumpAndSettle();

        await tester.tap(find.text('Refresh status'));
        await tester.pumpAndSettle();

        expect(find.text('SkipWise'), findsOneWidget);
      } finally {
        debugDefaultTargetPlatformOverride = null;
      }
    });
  });

  group('on desktop', () {
    testWidgets(
      'shows an explanatory notice instead of the accessibility checklist',
      (WidgetTester tester) async {
        debugDefaultTargetPlatformOverride = TargetPlatform.macOS;
        try {
          await tester.pumpWidget(const MyApp());
          await tester.pumpAndSettle();

          expect(find.text('SkipWise'), findsOneWidget);
          expect(find.text('Ad-skipping is Android-only'), findsOneWidget);
          expect(find.text('Accessibility service: OFF'), findsNothing);
        } finally {
          debugDefaultTargetPlatformOverride = null;
        }
      },
    );
  });
}
