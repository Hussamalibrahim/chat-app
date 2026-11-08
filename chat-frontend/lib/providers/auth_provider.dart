import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../config/api_constants.dart';
import '../models/user.dart';
import '../services/api_service.dart';
import '../services/token_storage.dart';

class AuthProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();

  UserModel? _currentUser;
  bool _isLoading = false;
  bool _isInitialized = false;
  String? _errorMessage;

  UserModel? get currentUser => _currentUser;
  bool get isLoading => _isLoading;
  bool get isInitialized => _isInitialized;
  bool get isAuthenticated => _currentUser != null;
  String? get errorMessage => _errorMessage;

  Future<void> initSession() async {
    _isLoading = true;
    notifyListeners();

    try {
      final token = await TokenStorage.getAccessToken();
      if (token != null && token.isNotEmpty) {
        final response = await _apiService.get(ApiConstants.currentUser);
        if (response.statusCode == 200) {
          final data = jsonDecode(response.body);
          _currentUser = UserModel.fromJson(data);
          await TokenStorage.saveUserInfo(
            userId: _currentUser!.id,
            username: _currentUser!.username,
          );
        } else {
          await TokenStorage.clear();
          _currentUser = null;
        }
      } else {
        _currentUser = null;
      }
    } catch (_) {
      _currentUser = null;
    } finally {
      _isLoading = false;
      _isInitialized = true;
      notifyListeners();
    }
  }

  Future<bool> login(String identifier, String password) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final response = await _apiService.post(
        ApiConstants.login,
        body: {'identifier': identifier.trim(), 'password': password},
        requiresAuth: false,
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final accessToken = data['accessToken'] as String;
        final refreshToken = data['refreshToken'] as String;

        await TokenStorage.saveTokens(
          accessToken: accessToken,
          refreshToken: refreshToken,
        );

        // Fetch current user details
        final userResponse = await _apiService.get(ApiConstants.currentUser);
        if (userResponse.statusCode == 200) {
          final userData = jsonDecode(userResponse.body);
          _currentUser = UserModel.fromJson(userData);
          await TokenStorage.saveUserInfo(
            userId: _currentUser!.id,
            username: _currentUser!.username,
          );
        }

        _isLoading = false;
        notifyListeners();
        return true;
      } else {
        _errorMessage = 'Invalid username or password';
        _isLoading = false;
        notifyListeners();
        return false;
      }
    } on ApiException catch (e) {
      _errorMessage = e.message;
      _isLoading = false;
      notifyListeners();
      return false;
    } catch (e) {
      _errorMessage = 'Login failed: ${e.toString()}';
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> register({
    required String username,
    String? email,
    String? phone,
    required String password,
  }) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final body = <String, dynamic>{
        'username': username.trim(),
        'password': password,
      };
      if (email != null && email.trim().isNotEmpty) {
        body['email'] = email.trim();
      }
      if (phone != null && phone.trim().isNotEmpty) {
        body['phone'] = phone.trim();
      }

      final response = await _apiService.post(
        ApiConstants.register,
        body: body,
        requiresAuth: false,
      );

      if (response.statusCode == 201) {
        // Automatically login with registered identifier
        final identifier = (email != null && email.trim().isNotEmpty)
            ? email.trim()
            : username.trim();
        return await login(identifier, password);
      } else {
        _errorMessage = 'Registration failed.';
        _isLoading = false;
        notifyListeners();
        return false;
      }
    } on ApiException catch (e) {
      _errorMessage = e.message;
      _isLoading = false;
      notifyListeners();
      return false;
    } catch (e) {
      _errorMessage = 'Registration failed: ${e.toString()}';
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<void> logout() async {
    try {
      final refreshToken = await TokenStorage.getRefreshToken();
      if (refreshToken != null && refreshToken.isNotEmpty) {
        await _apiService.post(
          ApiConstants.logout,
          body: {'refreshToken': refreshToken},
        );
      }
    } catch (_) {}

    await TokenStorage.clear();
    _currentUser = null;
    _errorMessage = null;
    notifyListeners();
  }
}

