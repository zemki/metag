import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:url_launcher/url_launcher.dart';
import 'models/entry_model.dart';
import 'services/entry_services.dart';
import 'newentry.dart';
import 'globals.dart' as globals;
import 'package:permission_handler/permission_handler.dart';

class HomePage extends StatefulWidget {
  static const String tag = 'login-page';

  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();
  late Future<List<Entry>> _future;

  @override
  void initState() {
    super.initState();
    debugPrint('HomePage initState started');
    _initializeHome();
  }

  Future<void> _initializeHome() async {
    try {
      // Start loading entries
      _future = EntryService.getAllEntries();

      // Initialize date formatting
      await initializeDateFormatting();

      // Check for permissions
      await _checkPermissions();

      if (!mounted) return;

      debugPrint('HomePage initialization complete');
    } catch (e) {
      debugPrint('Error during home initialization: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error initializing: $e'),
            duration: const Duration(seconds: 3),
          ),
        );
      }
    }
  }

  Future<void> _checkPermissions() async {
    // Don't check permissions in simulator
    if (Platform.isIOS) {
      final isSimulator = await _isSimulator();
      if (isSimulator) {
        debugPrint('Running on iOS simulator - skipping microphone permission');
        return;
      }
    }

    try {
      await Future.delayed(const Duration(milliseconds: 500));

      // Reset permission status first
      if (Platform.isIOS) {
        await Permission.microphone.shouldShowRequestRationale;
      }

      final micStatus = await Permission.microphone.status;
      debugPrint('Initial microphone permission status in home: $micStatus');

      if (micStatus.isDenied || micStatus.isRestricted) {
        if (!mounted) return;

        final shouldRequest = await showDialog<bool>(
          context: context,
          barrierDismissible: false,
          builder: (BuildContext context) => AlertDialog(
            title: const Text('Microphone Access Required'),
            content: const Text(
              'MeTag needs microphone access to record audio entries. '
              'This is required for studies that include voice recording.\n\n'
              'Please tap "Allow" when prompted for microphone access.',
            ),
            actions: [
              TextButton(
                child: const Text('Open Settings'),
                onPressed: () async {
                  Navigator.of(context).pop(false);
                  await openAppSettings();
                },
              ),
              TextButton(
                child: const Text('Continue'),
                onPressed: () => Navigator.of(context).pop(true),
              ),
            ],
          ),
        );

        if (shouldRequest ?? false) {
          // Request permission with proper error handling
          try {
            final result = await Permission.microphone.request();
            debugPrint('Permission request result: $result');

            if (result.isPermanentlyDenied) {
              if (!mounted) return;
              await showDialog(
                context: context,
                builder: (context) => AlertDialog(
                  title: const Text('Permission Required'),
                  content: const Text(
                    'Microphone access is required for recording. '
                    'Please enable it in Settings > Privacy > Microphone > MeTag',
                  ),
                  actions: [
                    TextButton(
                      child: const Text('Cancel'),
                      onPressed: () => Navigator.pop(context),
                    ),
                    TextButton(
                      child: const Text('Open Settings'),
                      onPressed: () async {
                        Navigator.pop(context);
                        await openAppSettings();
                      },
                    ),
                  ],
                ),
              );
            }
          } catch (e) {
            debugPrint('Error requesting permission: $e');
          }
        }
      }

      // Android specific permission handling
      if (Platform.isAndroid) {
        final micStatus = await Permission.microphone.status;
        debugPrint('Initial microphone permission status in home: $micStatus');

        if (micStatus.isDenied || micStatus.isRestricted) {
          if (!mounted) return;

          final shouldRequest = await showDialog<bool>(
            context: context,
            barrierDismissible: false,
            builder: (BuildContext context) => AlertDialog(
              title: const Text('Microphone Access Required'),
              content: const Text(
                'MeTag needs microphone access to record audio entries. '
                'This is required for studies that include voice recording.\n\n'
                'Please tap "Allow" when prompted for microphone access.',
              ),
              actions: [
                TextButton(
                  child: const Text('Open Settings'),
                  onPressed: () async {
                    Navigator.of(context).pop(false);
                    await openAppSettings();
                  },
                ),
                TextButton(
                  child: const Text('Continue'),
                  onPressed: () => Navigator.of(context).pop(true),
                ),
              ],
            ),
          );

          if (shouldRequest ?? false) {
            // Request permission with proper error handling
            try {
              final result = await Permission.microphone.request();
              debugPrint('Permission request result: $result');

              if (result.isPermanentlyDenied) {
                if (!mounted) return;
                await showDialog(
                  context: context,
                  builder: (context) => AlertDialog(
                    title: const Text('Permission Required'),
                    content: const Text(
                      'Microphone access is required for recording. '
                      'Please enable it in Settings > Privacy > Microphone > MeTag',
                    ),
                    actions: [
                      TextButton(
                        child: const Text('Cancel'),
                        onPressed: () => Navigator.pop(context),
                      ),
                      TextButton(
                        child: const Text('Open Settings'),
                        onPressed: () async {
                          Navigator.pop(context);
                          await openAppSettings();
                        },
                      ),
                    ],
                  ),
                );
              }
            } catch (e) {
              debugPrint('Error requesting permission: $e');
            }
          }
        }
      }
    } catch (e) {
      debugPrint('Error in permission check: $e');
    }
  }

// Helper method to detect simulator
  Future<bool> _isSimulator() async {
    if (Platform.isIOS) {
      try {
        final String result = await const MethodChannel('flutter/platform')
            .invokeMethod('isEmulator');
        return result == 'true';
      } catch (e) {
        debugPrint('Error checking simulator status: $e');
        return false;
      }
    }
    return false;
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
  }

  Future<void> _refreshAction() async {
    setState(() {
      _future = EntryService.getAllEntries();
    });
  }

  Future<bool> removeEntryFromList(Entry toremove) async {
    Fluttertoast.cancel();
    final duration = globals.duration.split(".");
    final lastProcessingDay = "${duration[2]}-${duration[1]}-${duration[0]}";

    if (DateTime.now().isAfter(DateTime.parse(lastProcessingDay))) {
      Fluttertoast.showToast(
        msg: globals.sofcontext.already_processed_data,
        toastLength: Toast.LENGTH_LONG,
        gravity: ToastGravity.CENTER,
        timeInSecForIosWeb: 5,
        backgroundColor: Colors.red,
        textColor: Colors.white,
        fontSize: 16.0,
      );
      return false;
    }

    final result = await EntryService.deleteEntry(toremove);
    if (result == "success") {
      await _refreshAction();
      await Fluttertoast.showToast(
        msg: globals.sofcontext.entry_deleted,
        toastLength: Toast.LENGTH_LONG,
        gravity: ToastGravity.BOTTOM,
        timeInSecForIosWeb: 2,
        backgroundColor: Colors.blue,
        textColor: Colors.white,
        fontSize: 16.0,
      );
      return true;
    }
    return false;
  }

  void choiceAction(String choice) async {
    if (choice == globals.sofcontext.homepage_menu_PP) {
      const url =
          'https://mesoftware.org/index.php/datenschutzerklaerung-metag';
      try {
        final Uri uri = Uri.parse(url);
        if (await canLaunchUrl(uri)) {
          await launchUrl(uri, mode: LaunchMode.externalApplication);
        } else {
          debugPrint('Could not launch $url');
        }
      } catch (e) {
        debugPrint('Error launching URL: $e');
      }
    } else if (choice == "Logout") {
      await _handleLogout();
    }
  }

  Future<void> _handleLogout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    if (mounted) {
      Navigator.of(context)
          .pushNamedAndRemoveUntil('/login', (Route<dynamic> route) => false);
    }
  }

  Future<void> showDialogifNotStarted() async {
    final prefs = await SharedPreferences.getInstance();
    final notStarted = prefs.getBool('notstarted') ?? false;

    if (notStarted && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(globals.sofcontext.data_collection_not_started),
          duration: const Duration(minutes: 100),
          action: SnackBarAction(
            label: globals.sofcontext.i_understand,
            onPressed: () {
              ScaffoldMessenger.of(context).hideCurrentSnackBar();
            },
          ),
        ),
      );
    }
  }

  Future<void> showDialogIfFirstLoaded() async {
    final prefs = await SharedPreferences.getInstance();
    final isFirstLoaded = prefs.getBool('is_first_loaded') ?? false;
    final notStarted = prefs.getBool('notstarted') ?? false;
    final currentCaseId = prefs.getString('currentcaseid');
    final oldCaseId = prefs.getString('oldcaseid');

    if (!notStarted &&
        (isFirstLoaded || oldCaseId != currentCaseId) &&
        mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            globals.sofcontext.data_future_elaboration + globals.duration,
          ),
          duration: const Duration(minutes: 100),
          action: SnackBarAction(
            label: globals.sofcontext.i_understand,
            onPressed: () async {
              await prefs.setBool('is_first_loaded', false);
              if (currentCaseId != null) {
                await prefs.setString('oldcaseid', currentCaseId);
              }
              if (mounted) {
                ScaffoldMessenger.of(context).hideCurrentSnackBar();
              }
            },
          ),
        ),
      );
    }
  }

  void showSnackbarIfExpired() {
    if (globals.duration.isEmpty) {
      debugPrint('Duration is empty, cannot check expiration');
      return;
    }

    final newDateList = globals.duration.split(".");
    if (newDateList.length != 3) {
      debugPrint('Invalid duration format: ${globals.duration}');
      return;
    }

    try {
      final newDate = "${newDateList[2]}-${newDateList[1]}-${newDateList[0]}";
      if (DateTime.now().isAfter(DateTime.parse(newDate)) && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              globals.sofcontext.data_elaborated + globals.duration,
            ),
          ),
        );
      }
    } catch (e) {
      debugPrint('Error processing date: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final choices = <String>[globals.sofcontext.homepage_menu_PP, "Logout"];

    return Scaffold(
      key: _scaffoldKey,
      floatingActionButton: FloatingActionButton(
        backgroundColor: ColorUtils.hexToColor("#113F63"),
        foregroundColor: Colors.white,
        onPressed: () => _navigateToNewEntry(),
        tooltip: globals.sofcontext.add_new_entry,
        child: const Icon(Icons.add),
      ),
      appBar: _buildAppBar(choices),
      body: _buildBody(),
    );
  }

  Future<void> _navigateToNewEntry() async {
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => const NewEntry(),
        fullscreenDialog: true,
      ),
    );
    if (result ?? false) {
      await _refreshAction();
    }
  }

  PreferredSizeWidget _buildAppBar(List<String> choices) {
    return AppBar(
      backgroundColor: Colors.white,
      leading: Image.asset('lib/assets/Android/Icon-512.png'),
      title: Text(globals.sofcontext.homepage_entries),
      actions: [
        PopupMenuButton<String>(
          onSelected: choiceAction,
          itemBuilder: (BuildContext context) {
            return choices.map((String choice) {
              return PopupMenuItem<String>(
                value: choice,
                child: Text(choice),
              );
            }).toList();
          },
        ),
      ],
    );
  }

  Widget _buildBody() {
    return RefreshIndicator(
      onRefresh: () async {
        await _refreshAction();
      },
      child: Container(
        child: _buildEntryList(),
      ),
    );
  }

  Widget _buildEntryList() {
    return FutureBuilder<List<Entry>>(
      future: _future,
      builder: (BuildContext context, AsyncSnapshot<List<Entry>> snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const LoadingSpinner();
        }

        if (!snapshot.hasData || snapshot.data!.isEmpty) {
          return const Center(child: Text('No entries available'));
        }

        final entries = snapshot.data!;
        entries.sort((a, b) =>
            DateTime.parse(b.begin).compareTo(DateTime.parse(a.begin)));

        Map<String, List<Entry>> groupedEntries = {};
        for (var entry in entries) {
          final date = DateTime.parse(entry.begin);
          final dateKey = '${date.year}-${date.month}-${date.day}';
          groupedEntries.putIfAbsent(dateKey, () => []).add(entry);
        }

        return ListView.builder(
          itemCount: groupedEntries.length,
          itemBuilder: (context, index) {
            final dateKey = groupedEntries.keys.elementAt(index);
            final dayEntries = groupedEntries[dateKey]!;
            final date = DateTime.parse(dayEntries[0].begin);

            return Column(
              children: [
                _buildDateHeader(date),
                ...dayEntries.map((entry) => _buildEntryItem(entry)).toList(),
              ],
            );
          },
        );
      },
    );
  }

  Widget _buildDateHeader(DateTime date) {
    return Stack(
      alignment: Alignment.center,
      children: [
        Image.asset(
          'lib/assets/header.jpg',
          height: 80,
          width: double.infinity,
          fit: BoxFit.cover,
        ),
        Container(
          color: Colors.black.withOpacity(0.4),
          height: 80,
          width: double.infinity,
        ),
        Text(
          _formatDate(date),
          style: const TextStyle(
            color: Colors.white,
            fontSize: 24,
            fontWeight: FontWeight.bold,
            shadows: [
              Shadow(
                offset: Offset(1.0, 1.0),
                blurRadius: 3.0,
                color: Colors.black45,
              ),
            ],
          ),
        ),
      ],
    );
  }

  String _formatDate(DateTime date) {
    final now = DateTime.now();
    if (date.year == now.year &&
        date.month == now.month &&
        date.day == now.day) {
      return 'Today';
    }

    final yesterday = now.subtract(const Duration(days: 1));
    if (date.year == yesterday.year &&
        date.month == yesterday.month &&
        date.day == yesterday.day) {
      return 'Yesterday';
    }

    return '${date.day}.${date.month}.${date.year}';
  }

  Widget _buildEntryItem(Entry entry) {
    final beginDate = DateTime.parse(entry.begin);
    final endDate = DateTime.parse(entry.end);

    final timeText = '${_formatTime(beginDate)} - ${_formatTime(endDate)}';

    return Slidable(
      key: ValueKey(entry.id),
      endActionPane: ActionPane(
        motion: const DrawerMotion(),
        children: [
          SlidableAction(
            onPressed: (_) => removeEntryFromList(entry),
            backgroundColor: Colors.red,
            foregroundColor: Colors.white,
            icon: Icons.delete,
            label: 'Delete',
          ),
        ],
      ),
      child: ListTile(
        onTap: () => _navigateToEditEntry(entry),
        title: Text(entry.media_name),
        subtitle: Text(timeText),
        trailing: const Icon(Icons.keyboard_arrow_right),
      ),
    );
  }

  String _formatTime(DateTime time) {
    final hour = time.hour.toString();
    final minute = time.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  Future<void> _navigateToEditEntry(Entry entry) async {
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => NewEntry(oldEntry: entry),
        fullscreenDialog: true,
      ),
    );
    if (result ?? false) {
      await _refreshAction();
    }
  }
}

class LoadingSpinner extends StatelessWidget {
  const LoadingSpinner({super.key});

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: SpinKitCubeGrid(
        color: Colors.blue,
        size: 50.0,
      ),
    );
  }
}

class ColorUtils {
  static Color hexToColor(String hexString, {String alphaChannel = 'FF'}) {
    return Color(
      int.parse(hexString.replaceFirst('#', '0x$alphaChannel')),
    );
  }
}
