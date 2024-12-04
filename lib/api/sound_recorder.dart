import 'package:flutter/material.dart';
import 'package:flutter_sound/flutter_sound.dart';
import 'package:audio_session/audio_session.dart';
import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';

class SoundRecorder {
  FlutterSoundRecorder? _audioRecorder;
  bool _isRecorderInitialized = false;
  bool _isPlaybackReady = false;
  bool _isRecording = false;
  String? _recordingPath;

  bool get isRecordingAvailable => _isPlaybackReady;
  bool get isRecording => _isRecording;
  String? get recordingPath => _recordingPath;

  Future<String> get _path async {
    final dir = await getApplicationDocumentsDirectory();
    return '${dir.path}/entry_recording.aac';
  }

  Future<void> init() async {
    _audioRecorder = FlutterSoundRecorder();

    try {
      final micStatus = await Permission.microphone.request();
      if (!micStatus.isGranted) {
        throw RecordingPermissionException('Microphone permission not granted');
      }

      final session = await AudioSession.instance;
      await session.configure(AudioSessionConfiguration(
        avAudioSessionCategory: AVAudioSessionCategory.playAndRecord,
        avAudioSessionCategoryOptions:
            AVAudioSessionCategoryOptions.allowBluetooth |
                AVAudioSessionCategoryOptions.defaultToSpeaker,
        avAudioSessionMode: AVAudioSessionMode.spokenAudio,
        avAudioSessionRouteSharingPolicy:
            AVAudioSessionRouteSharingPolicy.defaultPolicy,
        avAudioSessionSetActiveOptions: AVAudioSessionSetActiveOptions.none,
        androidAudioAttributes: const AndroidAudioAttributes(
          contentType: AndroidAudioContentType.speech,
          flags: AndroidAudioFlags.none,
          usage: AndroidAudioUsage.voiceCommunication,
        ),
        androidAudioFocusGainType: AndroidAudioFocusGainType.gain,
        androidWillPauseWhenDucked: true,
      ));

      await _audioRecorder!.openRecorder();
      _isRecorderInitialized = true;
      debugPrint('Recorder initialized successfully');
    } catch (e) {
      debugPrint('Error in recorder initialization: $e');
      throw RecordingPermissionException('Error initializing recorder: $e');
    }
  }

  Future<void> _record() async {
    if (!_isRecorderInitialized) return;

    try {
      await _clearExistingRecording();

      final path = await _path;
      _isRecording = true;

      await _audioRecorder!.startRecorder(
        toFile: path,
        codec: Codec.aacADTS,
        bitRate: 48000,
        sampleRate: 44100,
      );

      _recordingPath = path;
      debugPrint('Recording started at path: $path');
    } catch (e) {
      _isRecording = false;
      _recordingPath = null;
      debugPrint('Error starting recording: $e');
      rethrow;
    }
  }

  Future<void> _clearExistingRecording() async {
    try {
      final path = await _path;
      final file = File(path);
      if (await file.exists()) {
        await file.delete();
        debugPrint('Existing recording cleared');
      }
    } catch (e) {
      debugPrint('Error clearing existing recording: $e');
    }
  }

  Future<void> stop() async {
    if (!_isRecorderInitialized) return;

    try {
      String? result = await _audioRecorder!.stopRecorder();
      _isRecording = false;
      _isPlaybackReady = result != null;
      debugPrint('Recording stopped at path: $result');
    } catch (e) {
      debugPrint('Error stopping recording: $e');
      _isPlaybackReady = false;
      _isRecording = false;
    }
  }

  Future<void> toggleRecording(VoidCallback onRecordingChanged) async {
    try {
      if (_isRecording) {
        await stop();
      } else {
        await _record();
      }
      onRecordingChanged();
    } catch (e) {
      debugPrint('Error toggling recording: $e');
      _isRecording = false;
      onRecordingChanged();
    }
  }

  void dispose() {
    if (!_isRecorderInitialized) return;

    try {
      _audioRecorder!.closeRecorder();
      _audioRecorder = null;
      _isRecorderInitialized = false;
      _isPlaybackReady = false;
      _isRecording = false;
      _recordingPath = null;
    } catch (e) {
      debugPrint('Error disposing recorder: $e');
    }
  }
}
