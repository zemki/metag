// GENERATED CODE - DO NOT MODIFY BY HAND
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'intl/messages_all.dart';

// **************************************************************************
// Generator: Flutter Intl IDE plugin
// Made by Localizely
// **************************************************************************

// ignore_for_file: non_constant_identifier_names, lines_longer_than_80_chars
// ignore_for_file: join_return_with_assignment, prefer_final_in_for_each
// ignore_for_file: avoid_redundant_argument_values, avoid_escaping_inner_quotes

class S {
  S();

  static S? _current;

  static S get current {
    assert(_current != null,
        'No instance of S was loaded. Try to initialize the S delegate before accessing S.current.');
    return _current!;
  }

  static const AppLocalizationDelegate delegate = AppLocalizationDelegate();

  static Future<S> load(Locale locale) {
    final name = (locale.countryCode?.isEmpty ?? false)
        ? locale.languageCode
        : locale.toString();
    final localeName = Intl.canonicalizedLocale(name);
    return initializeMessages(localeName).then((_) {
      Intl.defaultLocale = localeName;
      final instance = S();
      S._current = instance;

      return instance;
    });
  }

  static S of(BuildContext context) {
    final instance = S.maybeOf(context);
    assert(instance != null,
        'No instance of S present in the widget tree. Did you add S.delegate in localizationsDelegates?');
    return instance!;
  }

  static S? maybeOf(BuildContext context) {
    return Localizations.of<S>(context, S);
  }

  /// `Add new Entry`
  String get add_new_entry {
    return Intl.message(
      'Add new Entry',
      name: 'add_new_entry',
      desc: '',
      args: [],
    );
  }

  /// `Your data are already been processed by the researcher`
  String get already_processed_data {
    return Intl.message(
      'Your data are already been processed by the researcher',
      name: 'already_processed_data',
      desc: '',
      args: [],
    );
  }

  /// `Begin date and time`
  String get beginDT {
    return Intl.message(
      'Begin date and time',
      name: 'beginDT',
      desc: '',
      args: [],
    );
  }

  /// `Your credentials are not valid.`
  String get credentials_not_valid {
    return Intl.message(
      'Your credentials are not valid.',
      name: 'credentials_not_valid',
      desc: '',
      args: [],
    );
  }

  /// `The collection of data is not yet started, you're unable to send data`
  String get data_collection_not_started {
    return Intl.message(
      'The collection of data is not yet started, you\'re unable to send data',
      name: 'data_collection_not_started',
      desc: '',
      args: [],
    );
  }

  /// `You cannot edit your data because they were processed by researcher on `
  String get data_elaborated {
    return Intl.message(
      'You cannot edit your data because they were processed by researcher on ',
      name: 'data_elaborated',
      desc: '',
      args: [],
    );
  }

  /// `Your data will be handled by the researcher and you won't be able to edit them anymore on `
  String get data_future_elaboration {
    return Intl.message(
      'Your data will be handled by the researcher and you won\'t be able to edit them anymore on ',
      name: 'data_future_elaboration',
      desc: '',
      args: [],
    );
  }

  /// `End date and time`
  String get endDT {
    return Intl.message(
      'End date and time',
      name: 'endDT',
      desc: '',
      args: [],
    );
  }

  /// `Enter Data`
  String get enter_data {
    return Intl.message(
      'Enter Data',
      name: 'enter_data',
      desc: '',
      args: [],
    );
  }

  /// `Entry was successfully deleted.`
  String get entry_deleted {
    return Intl.message(
      'Entry was successfully deleted.',
      name: 'entry_deleted',
      desc: '',
      args: [],
    );
  }

  /// `Update Entry`
  String get entry_update {
    return Intl.message(
      'Update Entry',
      name: 'entry_update',
      desc: '',
      args: [],
    );
  }

  /// `Your Entries`
  String get homepage_entries {
    return Intl.message(
      'Your Entries',
      name: 'homepage_entries',
      desc: '',
      args: [],
    );
  }

  /// `Privacy Policy`
  String get homepage_menu_PP {
    return Intl.message(
      'Privacy Policy',
      name: 'homepage_menu_PP',
      desc: '',
      args: [],
    );
  }

  /// `I understand.`
  String get i_understand {
    return Intl.message(
      'I understand.',
      name: 'i_understand',
      desc: '',
      args: [],
    );
  }

  /// `Remember Email?`
  String get login_email_remember {
    return Intl.message(
      'Remember Email?',
      name: 'login_email_remember',
      desc: '',
      args: [],
    );
  }

  /// `Forgot Password?`
  String get login_forgot_password {
    return Intl.message(
      'Forgot Password?',
      name: 'login_forgot_password',
      desc: '',
      args: [],
    );
  }

  /// `You have no cases. Contact your researcher.`
  String get login_no_cases {
    return Intl.message(
      'You have no cases. Contact your researcher.',
      name: 'login_no_cases',
      desc: '',
      args: [],
    );
  }

  /// `Please check your connectivity.`
  String get network_missing {
    return Intl.message(
      'Please check your connectivity.',
      name: 'network_missing',
      desc: '',
      args: [],
    );
  }

  /// `Now Recording`
  String get now_recording {
    return Intl.message(
      'Now Recording',
      name: 'now_recording',
      desc: '',
      args: [],
    );
  }

  /// `Override`
  String get override_recording {
    return Intl.message(
      'Override',
      name: 'override_recording',
      desc: '',
      args: [],
    );
  }

  /// `Password`
  String get password {
    return Intl.message(
      'Password',
      name: 'password',
      desc: '',
      args: [],
    );
  }

  /// `Play Recording`
  String get play_recording {
    return Intl.message(
      'Play Recording',
      name: 'play_recording',
      desc: '',
      args: [],
    );
  }

  /// `Please check your entries.`
  String get please_check_entries {
    return Intl.message(
      'Please check your entries.',
      name: 'please_check_entries',
      desc: '',
      args: [],
    );
  }

  /// `Please record an audio`
  String get please_record_audio {
    return Intl.message(
      'Please record an audio',
      name: 'please_record_audio',
      desc: '',
      args: [],
    );
  }

  /// `Please type or select a media`
  String get please_select_media {
    return Intl.message(
      'Please type or select a media',
      name: 'please_select_media',
      desc: '',
      args: [],
    );
  }

  /// `Please enter some text`
  String get please_write_text {
    return Intl.message(
      'Please enter some text',
      name: 'please_write_text',
      desc: '',
      args: [],
    );
  }

  /// `Press Start`
  String get press_start {
    return Intl.message(
      'Press Start',
      name: 'press_start',
      desc: '',
      args: [],
    );
  }

  /// `Your data are already been processed by the researcher`
  String get researcher_has_data {
    return Intl.message(
      'Your data are already been processed by the researcher',
      name: 'researcher_has_data',
      desc: '',
      args: [],
    );
  }

  /// `Please select one or more`
  String get select_one_or_more {
    return Intl.message(
      'Please select one or more',
      name: 'select_one_or_more',
      desc: '',
      args: [],
    );
  }

  /// `Please select only one option`
  String get select_only_one {
    return Intl.message(
      'Please select only one option',
      name: 'select_only_one',
      desc: '',
      args: [],
    );
  }

  /// `Sending Entry...`
  String get sending_entry {
    return Intl.message(
      'Sending Entry...',
      name: 'sending_entry',
      desc: '',
      args: [],
    );
  }

  /// `There is an error on the server, please contact the admin.`
  String get server_error {
    return Intl.message(
      'There is an error on the server, please contact the admin.',
      name: 'server_error',
      desc: '',
      args: [],
    );
  }

  /// `The service is not available, try again in few minutes. If this error persists, contact the admin.`
  String get service_not_available {
    return Intl.message(
      'The service is not available, try again in few minutes. If this error persists, contact the admin.',
      name: 'service_not_available',
      desc: '',
      args: [],
    );
  }

  /// `Your starting time is after the end time.`
  String get start_is_after_end {
    return Intl.message(
      'Your starting time is after the end time.',
      name: 'start_is_after_end',
      desc: '',
      args: [],
    );
  }

  /// `Stop Playing`
  String get stop_playing {
    return Intl.message(
      'Stop Playing',
      name: 'stop_playing',
      desc: '',
      args: [],
    );
  }
}

class AppLocalizationDelegate extends LocalizationsDelegate<S> {
  const AppLocalizationDelegate();

  List<Locale> get supportedLocales {
    return const <Locale>[
      Locale.fromSubtags(languageCode: 'en'),
      Locale.fromSubtags(languageCode: 'de'),
      Locale.fromSubtags(languageCode: 'it'),
    ];
  }

  @override
  bool isSupported(Locale locale) => _isSupported(locale);
  @override
  Future<S> load(Locale locale) => S.load(locale);
  @override
  bool shouldReload(AppLocalizationDelegate old) => false;

  bool _isSupported(Locale locale) {
    for (var supportedLocale in supportedLocales) {
      if (supportedLocale.languageCode == locale.languageCode) {
        return true;
      }
    }
    return false;
  }
}
