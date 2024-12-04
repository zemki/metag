import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:http/io_client.dart';

class CustomHttpClient {
  static http.Client createClient() {
    final client = HttpClient()
      ..badCertificateCallback = _badCertificateCallback;

    return IOClient(client);
  }

  static bool _badCertificateCallback(
      X509Certificate cert, String host, int port) {
    return true;
  }

  static Future<http.Response?> get(String url,
      {Map<String, String>? headers}) async {
    try {
      final client = createClient();
      final response = await client.get(
        Uri.parse(url),
        headers: headers,
      );
      client.close();
      return response;
    } catch (e) {
      print('HTTP Client Get Error: $e');
      return null;
    }
  }

  static Future<http.Response?> post(
    String url, {
    Map<String, String>? headers,
    Object? body,
  }) async {
    try {
      final client = createClient();
      final response = await client.post(
        Uri.parse(url),
        headers: headers,
        body: body,
      );
      client.close();
      return response;
    } catch (e) {
      print('HTTP Client Post Error: $e');
      return null;
    }
  }

  static Future<http.Response?> put(
    String url, {
    Map<String, String>? headers,
    Object? body,
  }) async {
    try {
      final client = createClient();
      final response = await client.put(
        Uri.parse(url),
        headers: headers,
        body: body,
      );
      client.close();
      return response;
    } catch (e) {
      print('HTTP Client Put Error: $e');
      return null;
    }
  }

  static Future<http.Response?> delete(
    String url, {
    Map<String, String>? headers,
    Object? body,
  }) async {
    try {
      final client = createClient();
      final response = await client.delete(
        Uri.parse(url),
        headers: headers,
        body: body,
      );
      client.close();
      return response;
    } catch (e) {
      print('HTTP Client Delete Error: $e');
      return null;
    }
  }

  static Future<http.Response?> patch(
    String url, {
    Map<String, String>? headers,
    Object? body,
  }) async {
    try {
      final client = createClient();
      final response = await client.patch(
        Uri.parse(url),
        headers: headers,
        body: body,
      );
      client.close();
      return response;
    } catch (e) {
      print('HTTP Client Patch Error: $e');
      return null;
    }
  }
}
