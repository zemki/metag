library metag.globals;

import 'package:flutter/material.dart';
import 'package:flutter_datetime_picker_plus/flutter_datetime_picker_plus.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:metag/generated/l10n.dart';
import 'package:path_provider/path_provider.dart';

// App State
bool isLoggedIn = false;
bool notStarted = false;

// Configuration
String get baseurl {
  final isDebug = dotenv.env['DEBUG']?.toLowerCase() == 'true';
  return isDebug
      ? dotenv.env['DEVURL'] ?? 'setitonenv'
      : dotenv.env['PRODURL'] ?? 'setitonenv';
}

const int audioRecorderLimit = 1800; // seconds
String get fileTransferKey => dotenv.env['FILE_TRANSFER_KEY'] ?? 'setitonenv';

// Runtime Values
String duration = "";
String localPath = "";

// Localization
late S sofcontext; // Will be initialized during app startup
String locale = 'en'; // Default to English
LocaleType localEnum = LocaleType.en; // Default to English

// Helper Methods
Locale getLocale(BuildContext context) {
  return Localizations.localeOf(context);
}

// Initialize global values
Future<void> initializeGlobals(BuildContext context) async {
  final dir = await getApplicationDocumentsDirectory();
  try {
    sofcontext = S.of(context);
    final currentLocale = getLocale(context);
    locale = currentLocale.languageCode;
    localEnum = getLocalEnumFromCode(locale);

    localPath = '${dir.path}';
  } catch (e) {
    debugPrint('Error initializing globals: $e');
    rethrow;
  }
}

// Convert language code to LocaleType
LocaleType getLocalEnumFromCode(String code) {
  switch (code) {
    case 'de':
      return LocaleType.de;
    case 'it':
      return LocaleType.it;
    default:
      return LocaleType.en;
  }
}

// Reset globals
void resetGlobals() {
  isLoggedIn = false;
  notStarted = false;
  duration = "";
  localPath = "";
  locale = 'en';
  localEnum = LocaleType.en;
}

// Constants for standard values
class GlobalConstants {
  static const defaultTimeout = Duration(seconds: 30);
  static const maxRetries = 3;
  static const supportedLocales = ['en', 'de', 'it'];
}

// Error messages
class GlobalErrors {
  static String getNetworkError(String locale) {
    switch (locale) {
      case 'de':
        return 'Netzwerkfehler';
      case 'it':
        return 'Errore di rete';
      default:
        return 'Network Error';
    }
  }
}
