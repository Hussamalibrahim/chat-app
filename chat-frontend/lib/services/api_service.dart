import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_constants.dart';
import 'token_storage.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;

  ApiException(this.statusCode, this.message);

  @override
  String toString() => message;
}

class ApiService {
  static final ApiService _instance = ApiService._internal();
  factory ApiService() => _instance;
  ApiService._internal();

  Future<Map<String, String>> _getHeaders({bool requiresAuth = true}) async {
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };

    if (requiresAuth) {
      final token = await TokenStorage.getAccessToken();
      if (token != null && token.isNotEmpty) {
        headers['Authorization'] = 'Bearer $token';
      }
    }

    return headers;
  }

  Future<http.Response> get(String url, {bool requiresAuth = true}) async {
    final headers = await _getHeaders(requiresAuth: requiresAuth);
    final response = await http.get(Uri.parse(url), headers: headers);
    return _handleResponse(response, () => get(url, requiresAuth: requiresAuth));
  }

  Future<http.Response> post(
    String url, {
    Map<String, dynamic>? body,
    bool requiresAuth = true,
  }) async {
    final headers = await _getHeaders(requiresAuth: requiresAuth);
    final response = await http.post(
      Uri.parse(url),
      headers: headers,
      body: body != null ? jsonEncode(body) : null,
    );
    return _handleResponse(
      response,
      () => post(url, body: body, requiresAuth: requiresAuth),
    );
  }

  Future<http.Response> patch(
    String url, {
    Map<String, dynamic>? body,
    bool requiresAuth = true,
  }) async {
    final headers = await _getHeaders(requiresAuth: requiresAuth);
    final response = await http.patch(
      Uri.parse(url),
      headers: headers,
      body: body != null ? jsonEncode(body) : null,
    );
    return _handleResponse(
      response,
      () => patch(url, body: body, requiresAuth: requiresAuth),
    );
  }

  Future<http.Response> delete(String url, {bool requiresAuth = true}) async {
    final headers = await _getHeaders(requiresAuth: requiresAuth);
    final response = await http.delete(Uri.parse(url), headers: headers);
    return _handleResponse(
      response,
      () => delete(url, requiresAuth: requiresAuth),
    );
  }

  Future<http.Response> _handleResponse(
    http.Response response,
    Future<http.Response> Function() retryCall,
  ) async {
    if (response.statusCode == 401) {
      final refreshed = await _tryRefreshToken();
      if (refreshed) {
        return await retryCall();
      } else {
        await TokenStorage.clear();
        throw ApiException(401, 'Session expired. Please log in again.');
      }
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      return response;
    }

    String errorMessage = 'Request failed with status: ${response.statusCode}';
    try {
      final json = jsonDecode(response.body);
      if (json is Map<String, dynamic>) {
        if (json.containsKey('message')) {
          errorMessage = json['message'].toString();
        } else if (json.containsKey('error')) {
          errorMessage = json['error'].toString();
        }
      }
    } catch (_) {}

    throw ApiException(response.statusCode, errorMessage);
  }

  Future<bool> _tryRefreshToken() async {
    try {
      final refreshToken = await TokenStorage.getRefreshToken();
      if (refreshToken == null || refreshToken.isEmpty) {
        return false;
      }

      final res = await http.post(
        Uri.parse(ApiConstants.refresh),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'refreshToken': refreshToken}),
      );

      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        final newAccess = data['accessToken'] as String?;
        final newRefresh = data['refreshToken'] as String?;
        if (newAccess != null && newRefresh != null) {
          await TokenStorage.saveTokens(
            accessToken: newAccess,
            refreshToken: newRefresh,
          );
          return true;
        }
      }
    } catch (_) {}
    return false;
  }
}

