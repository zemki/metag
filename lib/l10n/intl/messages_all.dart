// DO NOT EDIT. This is code generated via package:intl/generate_localized.dart
// This is a library that looks up messages for specific locales by
// delegating to the appropriate library.

import 'dart:async';
import 'package:intl/intl.dart';
import 'package:intl/message_lookup_by_library.dart';
import 'package:intl/src/intl_helpers.dart';
import 'messages_de.dart' as messages_de;
import 'messages_en.dart' as messages_en;

typedef Future<dynamic> LibraryLoader();

Map<String, LibraryLoader> _deferredLibraries = {
  'de': () => Future.value(null),
  'en': () => Future.value(null),
  'it': () => Future.value(null),
};

MessageLookupByLibrary? _findExact(String localeName) {
  switch (localeName) {
    case 'de':
      return messages_de.messages;
    case 'en':
      return messages_en.messages;
    default:
      return messages_en
          .messages; // Default to English instead of returning null
  }
}

/// User programs should call this before using [localeName] for messages.
Future<bool> initializeMessages(String localeName) async {
  final String? availableLocale = Intl.verifiedLocale(
    localeName,
    (locale) => _deferredLibraries.containsKey(locale),
    onFailure: (_) => 'en', // Default to English
  );

  if (availableLocale == '') {
    // Changed from isEmpty check to direct comparison
    return false;
  }

  final lib = _deferredLibraries[availableLocale];
  await lib?.call();

  initializeInternalMessageLookup(() => CompositeMessageLookup());
  messageLookup.addLocale(availableLocale!, _findGeneratedMessagesFor);

  return true;
}

bool _messagesExistFor(String locale) {
  try {
    return _findExact(locale) != null;
  } catch (e) {
    return false;
  }
}

MessageLookupByLibrary _findGeneratedMessagesFor(String locale) {
  final String? actualLocale = Intl.verifiedLocale(
    locale,
    _messagesExistFor,
    onFailure: (_) => 'en', // Default to English
  );

  return _findExact(actualLocale!) ??
      messages_en.messages; // Ensure we never return null
}
