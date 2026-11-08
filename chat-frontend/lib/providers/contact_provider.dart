import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../config/api_constants.dart';
import '../models/contact.dart';
import '../models/user.dart';
import '../services/api_service.dart';

class ContactProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();

  List<ContactModel> _contacts = [];
  List<UserModel> _searchedUsers = [];
  bool _isLoading = false;
  String? _errorMessage;

  List<ContactModel> get contacts => _contacts;
  List<UserModel> get searchedUsers => _searchedUsers;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  Future<void> fetchContacts() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final response = await _apiService.get(ApiConstants.contacts);
      if (response.statusCode == 200) {
        final List<dynamic> list = jsonDecode(response.body);
        _contacts = list.map((item) => ContactModel.fromJson(item)).toList();
      }
    } catch (e) {
      _errorMessage = 'Failed to load contacts: ${e.toString()}';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> addContact(String contactUserId, {String? alias}) async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _apiService.post(
        ApiConstants.contacts,
        body: {
          'contactUserId': contactUserId,
          if (alias != null && alias.trim().isNotEmpty) 'alias': alias.trim(),
        },
      );

      if (response.statusCode == 201) {
        await fetchContacts();
        return true;
      }
    } catch (e) {
      _errorMessage = 'Failed to add contact: ${e.toString()}';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
    return false;
  }

  Future<bool> removeContact(String contactUserId) async {
    try {
      final response =
          await _apiService.delete(ApiConstants.deleteContact(contactUserId));
      if (response.statusCode == 204) {
        _contacts.removeWhere((c) => c.contactUserId == contactUserId);
        notifyListeners();
        return true;
      }
    } catch (e) {
      _errorMessage = 'Failed to remove contact: ${e.toString()}';
      notifyListeners();
    }
    return false;
  }

  Future<void> searchUsers(String query) async {
    if (query.trim().isEmpty) {
      _searchedUsers = [];
      notifyListeners();
      return;
    }

    _isLoading = true;
    notifyListeners();

    try {
      final url = '${ApiConstants.searchUsers}?query=${Uri.encodeComponent(query.trim())}';
      final response = await _apiService.get(url);
      if (response.statusCode == 200) {
        final List<dynamic> list = jsonDecode(response.body);
        _searchedUsers = list.map((item) => UserModel.fromJson(item)).toList();
      }
    } catch (e) {
      _errorMessage = 'Search failed: ${e.toString()}';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void clearSearch() {
    _searchedUsers = [];
    notifyListeners();
  }
}

