import 'package:flutter/material.dart';
import 'package:flutter_sound/flutter_sound.dart';
import 'dart:io';
import '../globals.dart' as globals;

class SoundPlayer {
  FlutterSoundPlayer? _audioPlayer;
  bool _isPlayerInitialized = false;
  bool _isPlaying = false;
  Codec _codec = Codec.aacMP4;

  bool get isPlaying => _isPlaying;

  Future<void> init() async {
    _audioPlayer = FlutterSoundPlayer();
    _audioPlayer!.openPlayer().then((value) {
      _isPlayerInitialized = true;
    });
  }

  Future<void> _play(VoidCallback whenFinished) async {
    if (!_isPlayerInitialized) return;

    try {
      final String path = '${globals.localPath}/entry_recording.aac';
      final File audioFile = File(path);

      if (!await audioFile.exists()) {
        debugPrint('Audio file not found at: $path');
        return;
      }

      // For iOS, we need to use file:// schema
      final String fileUri = Platform.isIOS ? 'file://$path' : path;
      debugPrint('Starting playback from: $fileUri');

      _isPlaying = true;

      await _audioPlayer!.startPlayer(
        fromURI: fileUri,
        codec: _codec,
        whenFinished: () {
          _isPlaying = false;
          whenFinished();
          debugPrint('Playback finished');
        },
      );
      debugPrint('Started playing successfully');
    } catch (e) {
      _isPlaying = false;
      debugPrint('Error playing recording: $e');
      whenFinished();
    }
  }

  Future<void> _stop() async {
    if (!_isPlayerInitialized || !_isPlaying) return;

    try {
      await _audioPlayer!.stopPlayer();
      _isPlaying = false;
      debugPrint('Stopped playing');
    } catch (e) {
      debugPrint('Error stopping playback: $e');
    }
  }

  Future<void> togglePlaying({required VoidCallback whenFinished}) async {
    if (!_isPlayerInitialized) {
      debugPrint('Player not initialized');
      return;
    }

    try {
      if (_isPlaying) {
        await _stop();
        whenFinished();
      } else {
        await _play(whenFinished);
      }
    } catch (e) {
      debugPrint('Error toggling playback: $e');
      _isPlaying = false;
      whenFinished();
    }
  }

  void dispose() {
    if (!_isPlayerInitialized) return;

    try {
      _audioPlayer!.closePlayer();
      _audioPlayer = null;
      _isPlayerInitialized = false;
      _isPlaying = false;
    } catch (e) {
      debugPrint('Error disposing player: $e');
    }
  }
}
