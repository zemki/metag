import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:metag/main.dart';

void main() {
  testWidgets('Initial app test', (WidgetTester tester) async {
    await tester.pumpWidget(const MetagApp());

    // Add your test assertions here
    // For example:
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}
