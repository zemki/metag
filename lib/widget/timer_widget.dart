import 'dart:async';
import 'package:flutter/material.dart';

class TimerController extends ValueNotifier<bool> {
  TimerController({bool isPlaying = false}) : super(isPlaying);

  int _limit = 0; // Initialize with default value

  set limit(int value) {
    assert(value >= 0);
    _limit = value;
  }

  int get limit => _limit;

  void startTimer() => value = true;

  void stopTimer() => value = false;
}

class TimerWidget extends StatefulWidget {
  final TimerController controller;

  const TimerWidget({
    super.key,
    required this.controller,
  });

  @override
  _TimerWidgetState createState() => _TimerWidgetState();
}

class _TimerWidgetState extends State<TimerWidget> {
  Duration duration = Duration.zero;
  late Timer timer; // Mark as late since it's initialized in initState
  int limit = 0;

  @override
  void initState() {
    super.initState();
    timer = Timer(Duration.zero, () {}); // Initialize with a dummy timer

    widget.controller.addListener(() {
      if (widget.controller.value) {
        startTimer();
      } else {
        stopTimer();
      }
    });
  }

  void reset() => setState(() => duration = Duration.zero);

  void addTime() {
    const addSeconds = 1;

    setState(() {
      final seconds = duration.inSeconds + addSeconds;

      if (seconds < 0) {
        timer.cancel();
      } else {
        if (limit > 0 && limit <= seconds) {
          stopTimer();
        }
        duration = Duration(seconds: seconds);
      }
    });
  }

  void startTimer({bool resets = true}) {
    if (!mounted) return;
    if (resets) {
      reset();
    }

    timer = Timer.periodic(const Duration(seconds: 1), (_) => addTime());
  }

  void stopTimer({bool resets = true}) {
    if (!mounted) return;
    if (resets) {
      reset();
    }

    setState(() => timer.cancel());
  }

  @override
  Widget build(BuildContext context) => Center(child: buildTime());

  Widget buildTime() {
    String twoDigits(int n) => n.toString().padLeft(2, "0");
    final twoDigitMinutes = twoDigits(duration.inMinutes.remainder(60));
    final twoDigitSeconds = twoDigits(duration.inSeconds.remainder(60));

    return Text(
      '$twoDigitMinutes:$twoDigitSeconds',
      style: const TextStyle(
        fontSize: 40,
        fontWeight: FontWeight.bold,
        color: Colors.black,
      ),
    );
  }

  @override
  void dispose() {
    timer.cancel();
    super.dispose();
  }
}
