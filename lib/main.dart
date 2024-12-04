import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:overlay_support/overlay_support.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:metag/generated/l10n.dart';
import 'package:metag/login.dart';
import 'package:metag/home.dart';
import 'package:metag/newentry.dart';
import 'package:metag/firebase_options.dart';
import 'globals.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await dotenv.load(fileName: ".env");

  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );

  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  final prefs = await SharedPreferences.getInstance();
  final token = prefs.getString('token');
  isLoggedIn = token != null;

  runApp(const MetagApp());
}

class MetagApp extends StatelessWidget {
  const MetagApp({super.key});

  @override
  Widget build(BuildContext context) {
    return OverlaySupport(
      child: MaterialApp(
        title: 'MeTag',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          primarySwatch: Colors.lightBlue,
          scaffoldBackgroundColor: Colors.white,
          appBarTheme: const AppBarTheme(
            backgroundColor: Colors.white,
            iconTheme: IconThemeData(color: Color(0xFF113F63)),
            titleTextStyle: TextStyle(
              color: Color(0xFF113F63),
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          elevatedButtonTheme: ElevatedButtonThemeData(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF113F63),
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(32.0),
              ),
            ),
          ),
        ),
        localizationsDelegates: const [
          S.delegate,
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        supportedLocales: S.delegate.supportedLocales,
        builder: (context, child) {
          return InitialPage(child: child);
        },
        home: const AppContent(),
        onGenerateRoute: generateRoute,
      ),
    );
  }

  Route<dynamic>? generateRoute(RouteSettings settings) {
    switch (settings.name) {
      case '/home':
        return MaterialPageRoute(builder: (_) => const HomePage());
      case '/login':
        return MaterialPageRoute(builder: (_) => const LoginPage());
      case '/newentry':
        return MaterialPageRoute(builder: (_) => const NewEntry());
      default:
        return null;
    }
  }
}

class InitialPage extends StatefulWidget {
  final Widget? child;

  const InitialPage({super.key, this.child});

  @override
  State<InitialPage> createState() => _InitialPageState();
}

class _InitialPageState extends State<InitialPage> {
  bool _initialized = false;

  @override
  void didChangeDependencies() async {
    super.didChangeDependencies();

    if (!_initialized) {
      sofcontext = S.of(context);
      locale = Localizations.localeOf(context).languageCode;
      localEnum = getLocalEnumFromCode(locale);
      await initializeGlobals(context); // Call with context here
      setState(() {
        _initialized = true;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!_initialized) {
      return const MaterialApp(
        home: Scaffold(
          body: Center(
            child: CircularProgressIndicator(),
          ),
        ),
      );
    }
    return widget.child ?? const SizedBox.shrink();
  }
}

class AppContent extends StatelessWidget {
  const AppContent({super.key});

  @override
  Widget build(BuildContext context) {
    return isLoggedIn ? const HomePage() : const LoginPage();
  }
}

class MetagScrollBehavior extends ScrollBehavior {
  @override
  ScrollPhysics getScrollPhysics(BuildContext context) {
    return const BouncingScrollPhysics();
  }
}
