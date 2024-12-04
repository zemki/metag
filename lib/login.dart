import 'dart:convert';
import 'package:flutter/material.dart';
import 'models/push_notification.dart';
import 'services/http_client.dart';
import 'utils/color_utils.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:fluttertoast/fluttertoast.dart';

import 'package:overlay_support/overlay_support.dart' as overlay;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:devicelocale/devicelocale.dart';
import 'package:url_launcher/url_launcher_string.dart';

import '../generated/l10n.dart';

import 'bloc.dart';
import 'globals.dart' as globals;

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  // Constants
  static const _storageKey = 'email';

  // Services
  final _storage = const FlutterSecureStorage();
  final _bloc = Bloc();
  late final FirebaseMessaging _messaging;

  // Controllers
  late final TextEditingController _emailController;
  late final TextEditingController _passwordController;
  int _totalNotifications = 0;
  PushNotification? _notificationInfo;

  // State
  bool _isLoading = false;
  bool _rememberEmail = false;
  String? _currentEmail;

  @override
  void initState() {
    super.initState();
    _initializeApp();
  }

  Future<void> _initializeApp() async {
    _initializeControllers();
    await _initializeFirebase();
    await _loadSavedEmail();
    _setupNotificationListeners();
  }

  void _initializeControllers() {
    _emailController = TextEditingController();
    _passwordController = TextEditingController();
  }

  Future<void> _initializeFirebase() async {
    try {
      await Firebase.initializeApp();
      _messaging = FirebaseMessaging.instance;
      await _registerNotifications().timeout(
        const Duration(seconds: 5),
        onTimeout: () {
          debugPrint('Firebase notification registration timed out');
        },
      );
      await _checkInitialMessage();
    } catch (e) {
      debugPrint('Firebase initialization error: $e');
      // Continue app initialization even if Firebase fails
    }
  }

  Future<void> _loadSavedEmail() async {
    _currentEmail = await _storage.read(key: _storageKey);
    if (_currentEmail?.isNotEmpty ?? false) {
      setState(() {
        _emailController.text = _currentEmail!;
        _bloc.emailChanged(_currentEmail!);
        _rememberEmail = true;
      });
      _focusPasswordField();
    }
  }

  void _focusPasswordField() {
    Future.delayed(const Duration(milliseconds: 500), () {
      if (mounted) {
        FocusScope.of(context).nextFocus();
      }
    });
  }

  Future<void> _registerNotifications() async {
    final settings = await _messaging.requestPermission(
      alert: true,
      badge: true,
      provisional: false,
      sound: true,
    );

    if (settings.authorizationStatus == AuthorizationStatus.authorized) {
      await FirebaseMessaging.instance
          .setForegroundNotificationPresentationOptions(
        alert: true,
        badge: true,
        sound: true,
      );
    }
  }

  void _setupNotificationListeners() {
    FirebaseMessaging.onMessage.listen(_handleForegroundMessage);
    FirebaseMessaging.onMessageOpenedApp.listen(_handleBackgroundMessage);
  }

  Future<void> _checkInitialMessage() async {
    final RemoteMessage? initialMessage =
        await FirebaseMessaging.instance.getInitialMessage();
    if (initialMessage != null) {
      _handleBackgroundMessage(initialMessage);
    }
  }

  void _handleForegroundMessage(RemoteMessage message) {
    final notification = PushNotification(
      title: message.notification?.title,
      body: message.notification?.body,
    );
    _updateNotificationState(notification);
    _showNotificationOverlay(notification);
  }

  void _handleBackgroundMessage(RemoteMessage message) {
    final notification = PushNotification(
      title: message.notification?.title,
      body: message.notification?.body,
    );
    _updateNotificationState(notification);
  }

  void _updateNotificationState(PushNotification notification) {
    if (!mounted) return;
    setState(() {
      _notificationInfo = notification;
      _totalNotifications++;
    });
  }

  void _showNotificationOverlay(PushNotification notification) {
    if (!mounted) return;
    overlay.showSimpleNotification(
      Text(notification.title ?? ''),
      subtitle: Text(notification.body ?? ''),
      background: ColorUtils.hexToColor('#4856fd'),
      duration: Duration(seconds: 5 + (notification.body?.length ?? 0) ~/ 100),
    );
  }

  Future<void> _handleLogin() async {
    if (!_bloc.isValid || _isLoading) return;

    setState(() => _isLoading = true);

    try {
      String? deviceToken;
      try {
        if (Theme.of(context).platform == TargetPlatform.iOS) {
          deviceToken = await _messaging
              .getAPNSToken()
              .timeout(const Duration(seconds: 10));
        } else {
          deviceToken =
              await _messaging.getToken().timeout(const Duration(seconds: 10));
        }
        debugPrint('Device token obtained: ${deviceToken != null}');
      } catch (e) {
        debugPrint('Error getting device token: $e');
        // Continue without device token
      }

      final locale = await _getCurrentLocale();
      debugPrint('Starting login request');

      final loginData = await _performLogin(deviceToken, locale);

      if (loginData != null) {
        debugPrint('Login successful, saving data');
        await _saveLoginData(loginData);
        await _handleEmailStorage();
        _navigateToHome();
      } else {
        debugPrint('Login failed: loginData is null');
      }
    } catch (e, stack) {
      debugPrint('Login error: $e');
      debugPrint('Stack trace: $stack');
      _showErrorToast(999);
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<String> _getCurrentLocale() async {
    final locale = await Devicelocale.currentLocale ?? 'en';
    return locale.substring(0, 2);
  }

  Future<Map<String, dynamic>?> _performLogin(
      String? deviceToken, String locale) async {
    final body = jsonEncode({
      'email': _emailController.text,
      'password': _passwordController.text,
      'datetime': (DateTime.now().millisecondsSinceEpoch ~/ 1000).toString(),
      'deviceID': deviceToken,
    });

    final response = await CustomHttpClient.post(
      '${globals.baseurl}/api/login',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: body,
    );

    if (response == null) {
      debugPrint('Login request failed - no response');
      _showErrorToast(998);
      return null;
    }
    debugPrint('Login url: ${globals.baseurl}/api/login');
    debugPrint('Login response status: ${response.statusCode}');

    if (response.statusCode != 200) {
      debugPrint('Login failed with status: ${response.statusCode}');
      _showErrorToast(response.statusCode);
      return null;
    }

    try {
      final decoded = json.decode(response.body) as Map<String, dynamic>;
      if (decoded.isEmpty) {
        debugPrint('Login response was empty JSON object');
        return null;
      }
      return decoded;
    } catch (e) {
      debugPrint('Error decoding login response: $e');
      debugPrint('Response body was: ${response.body}');
      _showErrorToast(500);
      return null;
    }
  }

  Future<void> _saveLoginData(Map<String, dynamic> data) async {
    debugPrint('Saving login data...');
    final prefs = await SharedPreferences.getInstance();

    try {
      // Extract and validate duration first
      final duration = data['duration']?.toString();
      debugPrint('Duration from login response: $duration');

      if (duration == null || duration.isEmpty) {
        throw Exception('No duration received from server');
      }

      // Save duration to both places
      globals.duration = duration;
      await prefs.setString('duration', duration);

      // Save other data
      globals.notStarted = json.encode(data['notstarted']) == "true";
      await prefs.setString('token', data['token']);
      await prefs.setString('file_token', data['file_token'] ?? "0");
      await prefs.setString('case', json.encode(data['case']));
      await prefs.setString('inputs', json.encode(data['inputs']));
      await prefs.setString('custominputs', json.encode(data['custominputs']));
      await prefs.setBool('notstarted', globals.notStarted);
      await prefs.setString('currentcaseid', data['case']['id'].toString());

      // Verify saves
      final verifyDuration = prefs.getString('duration');
      debugPrint('Verified duration in prefs: $verifyDuration');
      debugPrint('Verified duration in globals: ${globals.duration}');

      final tempDir = await getApplicationDocumentsDirectory();
      globals.localPath = tempDir.path;
    } catch (e) {
      debugPrint('Error saving login data: $e');
      throw Exception('Failed to save login data: $e');
    }
  }

  Future<void> _handleEmailStorage() async {
    if (_rememberEmail) {
      await _storage.write(key: _storageKey, value: _emailController.text);
    } else {
      await _storage.delete(key: _storageKey);
    }
  }

  void _navigateToHome() {
    Navigator.of(context).pushReplacementNamed('/home');
  }

  void _showErrorToast(int statusCode) {
    String message;
    switch (statusCode) {
      case 401:
        message = S.of(context).credentials_not_valid;
        break;
      case 499:
        message = S.of(context).login_no_cases;
        break;
      case 500:
        message = S.of(context).server_error;
        break;
      case 503:
        message = S.of(context).service_not_available;
        break;
      case 998:
        message = S.of(context).network_missing;
        break;
      default:
        message = 'An unexpected error occurred. Please contact administrator.';
    }

    Fluttertoast.showToast(
      msg: message,
      toastLength: Toast.LENGTH_LONG,
      gravity: ToastGravity.CENTER,
      backgroundColor: Colors.red,
      textColor: Colors.white,
      fontSize: 16.0,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Stack(
        children: [
          _buildLoginForm(),
          if (_isLoading) _buildLoadingOverlay(),
        ],
      ),
    );
  }

  Widget _buildLoginForm() {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _buildLogo(),
            const SizedBox(height: 24.0),
            _buildTitle(),
            const SizedBox(height: 24.0),
            _buildEmailField(),
            const SizedBox(height: 8.0),
            _buildPasswordField(),
            const SizedBox(height: 12.0),
            _buildForgotPasswordLink(),
            _buildRememberEmailCheckbox(),
            _buildLoginButton(),
          ],
        ),
      ),
    );
  }

  Widget _buildLogo() {
    return Hero(
      tag: 'MetagLogo',
      child: CircleAvatar(
        backgroundColor: Colors.transparent,
        radius: 48.0,
        child: Image.asset('lib/assets/logo.png'),
      ),
    );
  }

  Widget _buildTitle() {
    return const Hero(
      tag: "METAG",
      child: Text(
        "METAG",
        style: TextStyle(fontSize: 32.0, fontWeight: FontWeight.bold),
        textAlign: TextAlign.center,
      ),
    );
  }

  Widget _buildEmailField() {
    return StreamBuilder<String>(
      stream: _bloc.email,
      builder: (context, snapshot) => TextField(
        controller: _emailController,
        keyboardType: TextInputType.emailAddress,
        autocorrect: false,
        autofocus: true,
        onChanged: _bloc.emailChanged,
        onEditingComplete: () => FocusScope.of(context).nextFocus(),
        decoration: InputDecoration(
          hintText: 'Email',
          errorText: snapshot.error?.toString(),
          contentPadding: const EdgeInsets.fromLTRB(20.0, 10.0, 20.0, 10.0),
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(32.0)),
        ),
      ),
    );
  }

  Widget _buildPasswordField() {
    return StreamBuilder<String>(
      stream: _bloc.password,
      builder: (context, snapshot) => TextField(
        controller: _passwordController,
        obscureText: true,
        autocorrect: false,
        onChanged: _bloc.passwordChanged,
        decoration: InputDecoration(
          hintText: S.of(context).password,
          errorText: snapshot.error?.toString(),
          contentPadding: const EdgeInsets.fromLTRB(20.0, 10.0, 20.0, 10.0),
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(32.0)),
        ),
      ),
    );
  }

  Widget _buildForgotPasswordLink() {
    return TextButton(
      onPressed: () => launchUrlString('${globals.baseurl}/password/reset'),
      child: Text(
        S.of(context).login_forgot_password,
        style: const TextStyle(color: Colors.blue),
      ),
    );
  }

  Widget _buildRememberEmailCheckbox() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(S.of(context).login_email_remember),
        Checkbox(
          value: _rememberEmail,
          onChanged: (value) => setState(() => _rememberEmail = value ?? false),
        ),
      ],
    );
  }

  Widget _buildLoginButton() {
    return StreamBuilder<bool>(
      stream: _bloc.submitCheck,
      builder: (context, snapshot) => ElevatedButton(
        onPressed: snapshot.hasData && !_isLoading ? _handleLogin : null,
        style: ElevatedButton.styleFrom(
          backgroundColor: ColorUtils.hexToColor("#113F63"),
          minimumSize: const Size(200.0, 42.0),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(32.0),
          ),
        ),
        child: Text(
          'Log in',
          style: const TextStyle(color: Colors.white),
        ),
      ),
    );
  }

  Widget _buildLoadingOverlay() {
    return Container(
      color: Colors.black54,
      child: const Center(
        child: CircularProgressIndicator(
          valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _bloc.dispose();
    super.dispose();
  }
}
