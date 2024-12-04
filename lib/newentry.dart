import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_datetime_picker_plus/flutter_datetime_picker_plus.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_typeahead/flutter_typeahead.dart';
import 'starDisplay.dart';
import 'package:metag/customPackages/custom_MultiSelect.dart';
import 'api/sound_player.dart';
import 'api/sound_recorder.dart';
import 'widget/timer_widget.dart';
import 'services/entry_services.dart';
import 'models/entry_model.dart';
import 'globals.dart' as globals;

class NewEntry extends StatefulWidget {
  final Entry? oldEntry;

  const NewEntry({super.key, this.oldEntry});

  @override
  State<NewEntry> createState() => _NewEntryState();
}

class EntryState {
  Entry entry;
  String? media;
  Timer? timer;
  Future<Map<dynamic, dynamic>>? data;
  Future<Widget>? customInputs;
  bool isLoading = false;

  EntryState({
    required this.entry,
    this.media,
    this.timer,
    this.data,
    this.customInputs,
  });

  EntryState copyWith({
    Entry? entry,
    String? media,
    Timer? timer,
    Future<Map<dynamic, dynamic>>? data,
    Future<Widget>? customInputs,
    bool? isLoading,
  }) {
    return EntryState(
      entry: entry ?? this.entry,
      media: media ?? this.media,
      timer: timer ?? this.timer,
      data: data ?? this.data,
      customInputs: customInputs ?? this.customInputs,
    )..isLoading = isLoading ?? this.isLoading;
  }
}

class _NewEntryState extends State<NewEntry> {
  final GlobalKey<ScaffoldMessengerState> _scaffoldMessengerKey =
      GlobalKey<ScaffoldMessengerState>();
  late EntryState _state;
  final timerController = TimerController();
  final recorder = SoundRecorder();
  final player = SoundPlayer();

  // Form state
  final _formNewEntryKey = GlobalKey<FormState>();
  final _typeAheadController = TextEditingController();

  // UI State
  String submitButtonText = 'Add New Entry';
  bool initialFill = true;
  bool initialDateFill = true;
  bool initialFillScale = true;
  bool initialFillOneChoice = true;
  bool initialFillMultiChoice = true;
  bool hasFileTransfer = false;
  bool fileIsMandatory = false;
  Duration duration = Duration.zero;
  Color _buttonColor = Colors.black54;

  // Form fields state
  bool editable = true;
  DateTime begin = DateTime.now();
  DateTime end = DateTime.now().add(const Duration(minutes: 5));
  String checkInitialFillOneChoiceName = '';
  String checkInitialFillMultiChoiceName = '';
  List<Widget> choices = [];
  int scaleInitialValue = 1;
  int multiAnswerIndex = 0;
  int oneChoiceAnswerIndex = 0;
  List<dynamic> multiselectData = [[]];
  List<dynamic> oneChoiceData = [[]];
  int eventualScaleValue = 0;

  @override
  void initState() {
    super.initState();
    _initializeState();
    _setupAudio();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _checkAndShowDeadlineBanner();
    });
  }

  Future<void> _checkAndShowDeadlineBanner() async {
    final isEnabled = await _isSubmitEnabled();
    if (!isEnabled && mounted) {
      final prefs = await SharedPreferences.getInstance();
      final duration = prefs.getString('duration');
      if (duration != null) {
        Future.delayed(Duration.zero, () {
          if (!mounted) return;
          final message = globals.sofcontext.data_elaborated + duration;
          _scaffoldMessengerKey.currentState?.clearMaterialBanners();
          _scaffoldMessengerKey.currentState?.showMaterialBanner(
            MaterialBanner(
              backgroundColor: Colors.red.shade100,
              content: Text(
                message,
                style: const TextStyle(
                  color: Colors.black87,
                  fontSize: 14,
                ),
              ),
              leading:
                  const Icon(Icons.warning_amber_rounded, color: Colors.red),
              actions: [
                TextButton(
                  onPressed: () {
                    _scaffoldMessengerKey.currentState
                        ?.hideCurrentMaterialBanner();
                  },
                  child: Text(globals.sofcontext.i_understand),
                ),
              ],
            ),
          );
        });
      }
    }
  }

  Future<void> _initializeState() async {
    _state = EntryState(
      entry: Entry(
        begin: begin,
        end: end,
      ),
    );
    _state.data = getAllInputs();
    _state.customInputs = getCustomInputs();

    if (widget.oldEntry != null) {
      _initializeWithOldEntry();
    }
  }

  Future<Map<dynamic, dynamic>> getAllInputs() async {
    final prefs = await SharedPreferences.getInstance();
    final inputs = json.decode(prefs.getString('inputs') ?? '{}');

    return {
      'media': (inputs['media'] as List)
          .map<DropdownInputs>((item) => DropdownInputs.fromJson(item))
          .toList(),
    };
  }

  Future<Widget> getCustomInputs() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonInputs =
        json.decode(json.decode(prefs.getString('custominputs') ?? '[]'));

    final customInputs = (jsonInputs as List)
        .map<CustomInputs>((item) => CustomInputs.fromJson(item))
        .toList();

    if (customInputs.isEmpty) {
      return const SizedBox.shrink();
    }

    return Column(
      children: customInputs.map((input) {
        return Padding(
          padding:
              const EdgeInsets.only(bottom: 24.0), // Consistent bottom padding
          child: _buildCustomInput(input),
        );
      }).toList(),
    );
  }

  Future<void> _setupAudio() async {
    await recorder.init();
    await player.init();
  }

  void _initializeWithOldEntry() {
    _state = _state.copyWith(
      entry: widget.oldEntry!,
      media: widget.oldEntry!.media?.toString(),
    );
    _typeAheadController.text = widget.oldEntry!.media_name;
    submitButtonText = globals.sofcontext.entry_update;

    // Update begin and end times from old entry
    if (widget.oldEntry?.begin != null) {
      begin = DateTime.parse(widget.oldEntry!.begin);
    }
    if (widget.oldEntry?.end != null) {
      end = DateTime.parse(widget.oldEntry!.end);
    }
  }

  Widget buildPlay() {
    final isPlaying = player.isPlaying;

    return ElevatedButton.icon(
      style: ElevatedButton.styleFrom(
        minimumSize: const Size(175, 50),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
      ),
      icon: Icon(isPlaying ? Icons.stop : Icons.play_arrow),
      label: Text(isPlaying
          ? globals.sofcontext.stop_playing
          : globals.sofcontext.play_recording),
      onPressed: () async {
        if (!recorder.isRecordingAvailable) return;

        await player.togglePlaying(whenFinished: () {
          setState(() {
            if (mounted) {
              _state.timer?.cancel();
              timerController.stopTimer();
              duration = Duration.zero;
            }
          });
        });

        setState(() {
          if (player.isPlaying) {
            timerController.startTimer();
            _state.timer = Timer.periodic(const Duration(seconds: 1), (_) {
              if (mounted) {
                setState(() {
                  duration = Duration(seconds: duration.inSeconds + 1);
                });
              }
            });
          } else {
            _state.timer?.cancel();
            timerController.stopTimer();
            duration = Duration.zero;
          }
        });
      },
    );
  }

  void _updateUI() {
    setState(() {});
  }

  Widget buildStart() {
    final isRecording = recorder.isRecording;
    String text = isRecording ? 'STOP' : 'START';
    if (recorder.isRecordingAvailable && text == 'START') {
      text = globals.sofcontext.override_recording;
    }

    return ElevatedButton.icon(
      style: ElevatedButton.styleFrom(
        minimumSize: const Size(175, 50),
        backgroundColor: isRecording ? Colors.red : Colors.white,
        foregroundColor: isRecording ? Colors.white : Colors.black,
      ),
      icon: Icon(isRecording ? Icons.stop : Icons.mic),
      label: Text(
        text,
        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
      ),
      onPressed: () async {
        if (player.isPlaying) return;

        await recorder.toggleRecording(() {
          setState(() {
            // Update UI state here
            if (recorder.isRecording) {
              timerController.startTimer();
              _state.timer = Timer.periodic(const Duration(seconds: 1), (_) {
                setState(() {
                  if (duration.inSeconds >= globals.audioRecorderLimit) {
                    recorder.stop();
                    timerController.stopTimer();
                    _state.timer?.cancel();
                  } else {
                    duration = Duration(seconds: duration.inSeconds + 1);
                  }
                });
              });
            } else {
              _state.timer?.cancel();
              timerController.stopTimer();
              duration = Duration.zero;
            }
          });
        });
      },
    );
  }

  Widget buildPlayer() {
    final text = recorder.isRecording
        ? globals.sofcontext.now_recording
        : globals.sofcontext.press_start;

    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        TimerWidget(controller: timerController),
        const SizedBox(height: 8),
        Text(text),
      ],
    );
  }

  @override
  void dispose() {
    _scaffoldMessengerKey.currentState?.hideCurrentMaterialBanner();
    recorder.dispose();
    player.dispose();
    _typeAheadController.dispose();
    _state.timer?.cancel();
    duration = Duration.zero;
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return PopScope<Object?>(
      canPop: true,
      onPopInvokedWithResult: (bool didPop, Object? result) async {
        if (didPop) {
          _scaffoldMessengerKey.currentState?.hideCurrentMaterialBanner();
          return;
        }

        _state = _state.copyWith(
          entry: Entry(
            begin: DateTime.now(),
            end: DateTime.now().add(const Duration(minutes: 5)),
          ),
        );
        _formNewEntryKey.currentState?.reset();

        final recordingFile = File('${globals.localPath}/entry_recording.aac');
        if (await recordingFile.exists()) {
          await recordingFile.delete();
        }
      },
      child: ScaffoldMessenger(
        key: _scaffoldMessengerKey,
        child: Scaffold(
          appBar: AppBar(
            backgroundColor: Colors.white,
            title: Text(
              globals.sofcontext.enter_data,
              style: TextStyle(color: ColorUtils.hexToColor("#113F63")),
            ),
            leading: IconButton(
              icon: const Icon(Icons.arrow_back),
              color: ColorUtils.hexToColor("#113F63"),
              onPressed: () => _handleBack(),
            ),
          ),
          body: Form(
            // Move Form here
            key: _formNewEntryKey,
            child: ListView(
              shrinkWrap: true,
              children: <Widget>[
                Container(
                  padding: const EdgeInsets.all(32.0),
                  child: FutureBuilder<Map<dynamic, dynamic>>(
                    future: _state.data,
                    builder: (context, snapshot) {
                      if (!snapshot.hasData) {
                        return const Center(child: CircularProgressIndicator());
                      }
                      return _buildForm(snapshot.data!);
                    },
                  ),
                )
              ],
            ),
          ),
        ),
      ),
    );
  }

  Future<bool> _handleBack() async {
    _scaffoldMessengerKey.currentState?.hideCurrentMaterialBanner();
    _state = _state.copyWith(
      entry: Entry(
        begin: DateTime.now(),
        end: DateTime.now().add(const Duration(minutes: 5)),
      ),
    );
    _formNewEntryKey.currentState?.reset();
    Fluttertoast.cancel();
    Navigator.pop(context, true);
    return true;
  }

  Widget _buildCustomInput(CustomInputs input) {
    switch (input.type) {
      case 'text':
        return _buildTextInput(input);
      case 'scale':
        return _buildScaleInput(input);
      case 'one choice':
        return _buildOneChoiceInput(input);
      case 'multiple choice':
        return _buildMultipleChoiceInput(input);
      case 'audio recording':
        hasFileTransfer = true;
        fileIsMandatory = input.mandatory;
        return const SizedBox.shrink();
      default:
        return const SizedBox.shrink();
    }
  }

  Widget _buildOneChoiceInput(CustomInputs input) {
    return MultiSelect(
      maxSelectableItems: 1,
      titleText: input.name,
      required: input.mandatory,
      errorText: globals.sofcontext.select_only_one,
      hintText: globals.sofcontext.select_only_one,
      dataSource: input.answers
          .map((e) => {
                "display": e,
                "value": e,
              })
          .toList(),
      textField: "display",
      valueField: "value",
      filterable: true,
      initialValue: _state.entry.inputs[input.name],
      validator: (value) {
        if (input.mandatory && (value == null || (value as List).isEmpty)) {
          return globals.sofcontext.select_only_one;
        }
        return null;
      },
      onSaved: (value) {
        if (value != null) {
          _state = _state.copyWith(
            entry: _state.entry.copyWith(
              inputs: {..._state.entry.inputs, input.name: value},
            ),
          );
        }
      },
    );
  }

  Widget _buildMultipleChoiceInput(CustomInputs input) {
    return MultiSelect(
      titleText: input.name,
      required: input.mandatory,
      errorText: globals.sofcontext.select_one_or_more,
      hintText: globals.sofcontext.select_one_or_more,
      dataSource: input.answers
          .map((e) => {
                "display": e,
                "value": e,
              })
          .toList(),
      textField: "display",
      valueField: "value",
      filterable: true,
      initialValue: _state.entry.inputs[input.name],
      validator: (value) {
        if (input.mandatory && (value == null || (value as List).isEmpty)) {
          return globals.sofcontext.select_one_or_more;
        }
        return null;
      },
      onSaved: (value) {
        if (value != null) {
          _state = _state.copyWith(
            entry: _state.entry.copyWith(
              inputs: {..._state.entry.inputs, input.name: value},
            ),
          );
        }
      },
    );
  }

  Widget _buildTextInput(CustomInputs input) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text(
              input.name,
              style: const TextStyle(
                color: Colors.black54,
                fontSize: 14.0,
              ),
            ),
            if (input.mandatory)
              Text(
                ' *',
                style: TextStyle(
                  color: Colors.red.shade700,
                  fontSize: 12.0,
                ),
              ),
          ],
        ),
        TextFormField(
          maxLines: 6,
          minLines: 1,
          autocorrect: false,
          initialValue: _state.entry.inputs[input.name]?.toString() ?? '',
          decoration: const InputDecoration(
            border: OutlineInputBorder(),
          ),
          validator: (value) {
            if (input.mandatory && (value?.isEmpty ?? true)) {
              return globals.sofcontext.please_write_text;
            }
            return null;
          },
          onSaved: (content) {
            if (content != null) {
              _state = _state.copyWith(
                entry: _state.entry.copyWith(
                  inputs: {..._state.entry.inputs, input.name: content},
                ),
              );
            }
          },
        ),
      ],
    );
  }

  Widget _buildScaleInput(CustomInputs input) {
    return FormField<int>(
      initialValue: _state.entry.inputs[input.name] ?? 0,
      builder: (state) {
        return Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  input.name,
                  style: const TextStyle(
                    color: Colors.black54,
                    fontWeight: FontWeight.normal,
                  ),
                ),
                if (input.mandatory)
                  Text(
                    ' *',
                    style: TextStyle(
                      color: Colors.red.shade700,
                      fontSize: 12.0,
                    ),
                  ),
              ],
            ),
            StarRating(
              value: state.value ?? 0,
              onChanged: (value) {
                state.didChange(value);
                _state = _state.copyWith(
                  entry: _state.entry.copyWith(
                    inputs: {..._state.entry.inputs, input.name: value},
                  ),
                );
              },
            ),
            if (state.hasError)
              Text(
                state.errorText!,
                style: TextStyle(
                  color: Theme.of(context).colorScheme.error,
                ),
              ),
          ],
        );
      },
      validator: (value) => (input.mandatory && (value ?? 0) < 1)
          ? 'This field is required'
          : null,
    );
  }

  Widget _buildForm(Map<dynamic, dynamic> inputs) {
    final medias = inputs['media'] as List<DropdownInputs>;

    return Column(
      children: [
        if (hasFileTransfer) ...[
          Text(
            _state.entry.media_name,
            maxLines: 3,
            style: const TextStyle(
              color: Colors.black54,
              fontWeight: FontWeight.normal,
            ),
          ),
          buildPlayer(),
          const SizedBox(height: 16),
          buildStart(),
          if (recorder.isRecordingAvailable) ...[
            const SizedBox(height: 16),
            buildPlay(),
          ],
          const SizedBox(height: 24),
        ],
        _buildDateTimeFields(),
        const SizedBox(height: 24),
        _buildMediaSelector(medias),
        const SizedBox(height: 24),
        const Padding(padding: EdgeInsets.all(10.0)),
        FutureBuilder<Widget>(
          future: _state.customInputs,
          builder: (context, snapshot) {
            if (!snapshot.hasData) return const SizedBox.shrink();
            return snapshot.data!;
          },
        ),
        _buildSubmitButton(),
      ],
    );
  }

  Widget _buildMediaSelector(List<DropdownInputs> medias) {
    if (medias.isEmpty) {
      return const SizedBox.shrink();
    }

    debugPrint(
        'Available medias: ${medias.map((m) => '${m.id}: ${m.name}').join(', ')}');

    return TypeAheadField<DropdownInputs>(
      suggestionsCallback: (pattern) async {
        return medias
            .where((media) =>
                media.name.toLowerCase().contains(pattern.toLowerCase()))
            .toList();
      },
      itemBuilder: (context, media) {
        return ListTile(
          title: Text(media.name),
        );
      },
      onSelected: (media) {
        debugPrint(
            'Selected from dropdown - ID: ${media.id}, Name: ${media.name}');
        setState(() {
          _state = _state.copyWith(
            entry: _state.entry.copyWith(
              media: media.id.toString(),
              media_name: media.name,
            ),
          );
          _typeAheadController.text = media.name;
        });
      },
      builder: (context, suggestionsBoxController, focusNode) {
        return TextFormField(
          controller: _typeAheadController,
          focusNode: focusNode,
          decoration: const InputDecoration(
            labelText: 'Media',
            border: OutlineInputBorder(),
          ),
          onChanged: (value) {
            // For typed input, check if it exactly matches any media name
            final matchingMedia = medias.firstWhere(
              (media) => media.name.toLowerCase() == value.toLowerCase(),
              orElse: () => DropdownInputs(
                  id: -1, name: value), // Use -1 to indicate typed value
            );

            setState(() {
              _state = _state.copyWith(
                entry: _state.entry.copyWith(
                  // If we found a match, use its ID, otherwise use the typed text as media
                  media: matchingMedia.id != -1
                      ? matchingMedia.id.toString()
                      : value,
                  media_name: value,
                ),
              );
            });
            debugPrint(
                'Media changed - ID/Text: ${matchingMedia.id != -1 ? matchingMedia.id : value}, Name: $value');
          },
          validator: (value) {
            if (value == null || value.isEmpty) {
              return globals.sofcontext.please_select_media;
            }
            return null;
          },
          onSaved: (value) {
            if (value != null) {
              // Same logic as onChanged
              final matchingMedia = medias.firstWhere(
                (media) => media.name.toLowerCase() == value.toLowerCase(),
                orElse: () => DropdownInputs(id: -1, name: value),
              );

              _state = _state.copyWith(
                entry: _state.entry.copyWith(
                  media: matchingMedia.id != -1
                      ? matchingMedia.id.toString()
                      : value,
                  media_name: value,
                ),
              );
              debugPrint(
                  'Media saved - ID/Text: ${matchingMedia.id != -1 ? matchingMedia.id : value}, Name: $value');
            }
          },
        );
      },
    );
  }

  Widget _buildDateTimeFields() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          globals.sofcontext.beginDT,
          style: const TextStyle(
              color: Colors.black54, fontWeight: FontWeight.normal),
        ),
        _buildDateTimePicker(
          value: begin,
          onChanged: (date) => setState(() => begin = date),
        ),
        Text(
          globals.sofcontext.endDT,
          style: const TextStyle(
              color: Colors.black54, fontWeight: FontWeight.normal),
        ),
        _buildDateTimePicker(
          value: end,
          onChanged: (date) => setState(() => end = date),
        ),
      ],
    );
  }

  Widget _buildDateTimePicker({
    required DateTime value,
    required ValueChanged<DateTime> onChanged,
  }) {
    return TextButton(
      onPressed: () {
        DatePicker.showDateTimePicker(
          context,
          showTitleActions: true,
          onChanged: onChanged,
          onConfirm: onChanged,
          currentTime: value,
          locale: globals.localEnum,
        );
      },
      child: Text(
        _formatDateTime(value),
        style: TextStyle(color: _buttonColor, fontSize: 24),
      ),
    );
  }

  String _formatDateTime(DateTime date) {
    return "${date.day}.${date.month}.${date.year} "
        "${date.hour}:${date.minute.toString().padLeft(2, '0')}";
  }

  Widget _buildSubmitButton() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16.0),
      child: FutureBuilder<bool>(
        future: _isSubmitEnabled(),
        builder: (context, snapshot) {
          final isEnabled = snapshot.data ?? false;
          return ElevatedButton(
            onPressed: isEnabled ? _handleSubmit : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: _buttonColor,
              foregroundColor: Colors.white,
            ),
            child: Text(submitButtonText),
          );
        },
      ),
    );
  }

  Future<bool> _isSubmitEnabled() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final duration = prefs.getString('duration');

      debugPrint('Duration from prefs: $duration');

      if (duration == null || duration.isEmpty) {
        debugPrint('Duration is empty, cannot check expiration');
        return false;
      }

      final newDateList = duration.split(".");
      if (newDateList.length != 3) {
        debugPrint('Invalid duration format: $duration');
        return false;
      }

      // Create date string in YYYY-MM-DD format
      final newDate =
          "${newDateList[2]}-${newDateList[1].padLeft(2, '0')}-${newDateList[0].padLeft(2, '0')}";
      final deadlineDate = DateTime.parse(newDate);

      // Set time to end of day
      final deadlineDateTime = DateTime(
          deadlineDate.year, deadlineDate.month, deadlineDate.day, 23, 59, 59);

      return DateTime.now().isBefore(deadlineDateTime);
    } catch (e) {
      debugPrint('Error checking submission enabled: $e');
      return false;
    }
  }

  Future<void> _handleSubmit() async {
    if (!_validateForm()) return;

    final originalButtonText = submitButtonText; // Store original text
    setState(() => submitButtonText = globals.sofcontext.sending_entry);

    try {
      _formNewEntryKey.currentState?.save();

      final result = await EntryService.createEntry(
        _state.entry.copyWith(
          begin: begin,
          end: end,
        ),
        _formNewEntryKey,
        context,
      );

      if (result.isNotEmpty) {
        final jsonResult = json.decode(result);
        if (jsonResult['id'] != null) {
          _state = _state.copyWith(
            entry: _state.entry.copyWith(id: jsonResult['id']),
          );

          // Show success message with correct text
          Fluttertoast.showToast(
            msg: _state.entry.id != null
                ? "Entry successfully updated"
                : "Entry successfully created",
            toastLength: Toast.LENGTH_LONG,
            gravity: ToastGravity.BOTTOM,
            backgroundColor: Colors.green,
            textColor: Colors.white,
            fontSize: 16.0,
          );

          if (mounted) {
            Navigator.pop(context, true);
          }
        }
      }
    } catch (e) {
      _showErrorToast(globals.sofcontext.server_error);
    } finally {
      if (mounted) {
        setState(() => submitButtonText =
            originalButtonText); // Restore original text if error
      }
    }
  }

  bool _validateForm() {
    if (!(_formNewEntryKey.currentState?.validate() ?? false)) {
      _showErrorToast(globals.sofcontext.please_check_entries);
      return false;
    }

    if (begin.isAfter(end)) {
      _showErrorToast(globals.sofcontext.start_is_after_end);
      return false;
    }

    // Only check for mandatory audio if this is a new entry (not updating)
    if (fileIsMandatory &&
        widget.oldEntry == null &&
        !recorder.isRecordingAvailable) {
      _showErrorToast(globals.sofcontext.please_record_audio);
      return false;
    }

    return true;
  }

  void _showErrorToast(String message) {
    Fluttertoast.showToast(
      msg: message,
      toastLength: Toast.LENGTH_SHORT,
      gravity: ToastGravity.CENTER,
      timeInSecForIosWeb: 3,
      backgroundColor: Colors.red,
      textColor: Colors.white,
      fontSize: 16.0,
    );
  }
}

class ColorUtils {
  static Color hexToColor(String hexString, {String alphaChannel = 'FF'}) {
    return Color(int.parse(hexString.replaceFirst('#', '0x$alphaChannel')));
  }
}
