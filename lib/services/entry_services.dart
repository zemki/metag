import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:http/http.dart' as http;
import 'dart:async';
import 'dart:io';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/entry_model.dart';
import '../globals.dart' as globals;
import 'package:http/io_client.dart';

import 'http_client.dart';

class HttpService {
  static http.Client createClient() {
    var ioClient = HttpClient()..badCertificateCallback = _certificateCheck;
    return IOClient(ioClient);
  }

  static bool _certificateCheck(X509Certificate cert, String host, int port) =>
      true;
}

class EntryService {
  static final String _baseUrl = '${globals.baseurl}/api/v1';
  static final String _entriesEndpoint = '$_baseUrl/entry';
  static final String _casesEndpoint = '$_baseUrl/cases';

  static Future<List<Entry>> getAllEntries() async {
    debugPrint('1. Starting getAllEntries');
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('token');
      final casesStr = prefs.getString('case');
      debugPrint('2. Got preferences');

      if (token == null) {
        debugPrint('No token found');
        return [];
      }

      final cases = json.decode(casesStr ?? '{}');
      debugPrint('3. Decoded cases');

      if (cases == "No cases") {
        return noCase("custom message yet not used");
      }

      final headers = {
        HttpHeaders.authorizationHeader: "Bearer $token",
        HttpHeaders.acceptCharsetHeader: "application/json",
        'Content-Type': 'application/json'
      };

      final url = '$_entriesEndpoint/${cases['id']}';
      debugPrint('4. Making request to: $url');

      // Using CustomHttpClient instead of standard http.Client
      final response = await CustomHttpClient.get(
        url,
        headers: headers,
      );
      debugPrint('5. Got response');

      if (response == null) {
        debugPrint('No response received');
        return [];
      }

      if (response.statusCode == 200) {
        debugPrint('6. Success response');
        return allEntriesFromJson(response.body);
      }

      debugPrint('Error status: ${response.statusCode}');
      debugPrint('Error body: ${response.body}');
      return [];
    } catch (e, stack) {
      debugPrint('Error in getAllEntries: $e');
      debugPrint('Stack trace: $stack');
      return [];
    }
  }

  static Future<Map<dynamic, dynamic>> getAllInputs() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final inputsStr = prefs.getString('inputs');

      if (inputsStr == null) {
        return {'media': <DropdownInputs>[]};
      }

      final inputs = json.decode(inputsStr);

      // Transform the media list into DropdownInputs objects
      if (inputs['media'] is List) {
        inputs['media'] = (inputs['media'] as List)
            .map<DropdownInputs>((item) => DropdownInputs.fromJson(item))
            .toList();
      } else {
        inputs['media'] = <DropdownInputs>[];
      }

      return inputs;
    } catch (e) {
      debugPrint('Error getting inputs: $e');
      return {'media': <DropdownInputs>[]};
    }
  }

  static Future<String> deleteEntry(Entry entry) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('token');

      if (token == null) throw Exception('No token found');

      final headers = {
        HttpHeaders.authorizationHeader: "Bearer $token",
        HttpHeaders.acceptCharsetHeader: "application/json",
        'Content-Type': 'application/json'
      };

      final response = await HttpService.createClient().delete(
        Uri.parse('$_casesEndpoint/${entry.caseId}/entries/${entry.id}'),
        headers: headers,
      );

      if (response.statusCode == 200) {
        return "success";
      }
      throw Exception('Failed to delete entry: ${response.statusCode}');
    } catch (e) {
      debugPrint('Error deleting entry: $e');
      showToast(0);
      return "error";
    }
  }

  static Future<String> createEntry(Entry entry, GlobalKey<FormState> formKey,
      [BuildContext? context]) async {
    try {
      // Get the case ID from preferences
      final prefs = await SharedPreferences.getInstance();
      final caseIdStr = prefs.getString('currentcaseid');
      final caseId = int.tryParse(caseIdStr ?? '');

      // Update the entry with the case ID
      entry = entry.copyWith(caseId: caseId);

      debugPrint('Creating entry with media: ${entry.media}'); // Debug log

      final headers = await _prepareHeaders();

      // Handle audio file if exists
      if (await File('${globals.localPath}/entry_recording.aac').exists()) {
        final fileBytes = await File('${globals.localPath}/entry_recording.aac')
            .readAsBytes();
        final base64String = base64Encode(fileBytes);
        entry = entry.copyWith(audio: 'data:audio/mp3;base64,$base64String');
      }

      final String json = entryToJson(entry);
      debugPrint('Sending entry JSON: $json'); // Debug log
      final response = await _makeRequest(entry, headers, json);

      if (response == null) throw Exception('No response from server');

      if (response.statusCode == 200) {
        return response.body;
      } else if (response.statusCode == 302 && context != null) {
        //await _handleSessionExpired(context);
      }

      showToast(response.statusCode);
      return response.body;
    } catch (e) {
      debugPrint('Error creating entry: $e');
      showToast(0);
      return '';
    }
  }

  static Future<Map<String, String>> _prepareHeaders() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');

    if (token == null) {
      throw Exception('No token found');
    }
    debugPrint('Token: $token');

    // Start with base headers
    final headers = {
      HttpHeaders.authorizationHeader: "Bearer $token",
      HttpHeaders.acceptCharsetHeader: "application/json",
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };

    // First check if file token exists in prefs
    final fileToken = prefs.getString('file_token');
    if (fileToken != null && fileToken != '0') {
      // If we have a file token, check if audio file exists
      if (await File('${globals.localPath}/entry_recording.aac').exists()) {
        debugPrint('Adding file token: $fileToken');
        headers['x-file-token'] = fileToken;
      }
    }

    return headers;
  }

  static Future<http.Response?> _makeRequest(
      Entry entry, Map<String, String> headers, String body) async {
    final client = HttpService.createClient();
    try {
      if (entry.id == null) {
        return await client.post(
          Uri.parse('$_casesEndpoint/${entry.caseId}/entries'),
          headers: headers,
          body: body,
        );
      } else {
        return await client.patch(
          Uri.parse('$_casesEndpoint/${entry.caseId}/entries/${entry.id}'),
          headers: headers,
          body: body,
        );
      }
    } finally {
      client.close();
    }
  }

  static Future<void> _handleSessionExpired(BuildContext context) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    Navigator.of(context)
        .pushNamedAndRemoveUntil('/login', (Route<dynamic> route) => false);
  }
}

class DropdownInputs {
  final int id;
  final String name;

  const DropdownInputs({
    required this.id,
    required this.name,
  });

  factory DropdownInputs.fromJson(Map<String, dynamic> json) {
    return DropdownInputs(
      id: json['id'] ?? 0,
      name: json['name'] ?? '',
    );
  }
}

class CustomInputs {
  final String id;
  final String type;
  final String name;
  final bool mandatory;
  final dynamic answers;
  dynamic value;

  CustomInputs({
    required this.id,
    required this.type,
    required this.name,
    required this.mandatory,
    this.answers,
    this.value,
  });

  factory CustomInputs.fromJson(Map<String, dynamic> json) {
    return CustomInputs(
      id: json['id'] ?? '',
      type: json['type'] ?? '',
      name: json['name'] ?? '',
      mandatory: json['mandatory'] ?? false,
      answers: json['answers'],
      value: json['value'],
    );
  }
}

void showToast(int statusCode) {
  final message = _getToastMessage(statusCode);
  Fluttertoast.showToast(
    msg: message,
    toastLength: Toast.LENGTH_SHORT,
    gravity: ToastGravity.CENTER,
    timeInSecForIosWeb: statusCode == 500 || statusCode == 503 ? 5 : 1,
    backgroundColor: Colors.red,
    textColor: Colors.white,
    fontSize: 16.0,
  );
}

String _getToastMessage(int statusCode) {
  switch (statusCode) {
    case 200:
      return "Your operation was successful!";
    case 401:
      return "Your credentials are not valid.";
    case 500:
      return "There is an error on the server, please contact the admin.";
    case 503:
      return "The service is not available, try again in few minutes. If this error persists, contact the admin.";
    default:
      return "General error: $statusCode";
  }
}
