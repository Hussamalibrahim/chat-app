import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../config/api_constants.dart';
import '../models/chat_room.dart';
import '../models/message.dart';
import '../services/api_service.dart';

class ChatProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();

  List<ChatRoomModel> _chats = [];
  final Map<String, List<MessageModel>> _messages = {};
  bool _isLoadingChats = false;
  bool _isLoadingMessages = false;
  bool _isSending = false;
  String? _errorMessage;

  List<ChatRoomModel> get chats => _chats;
  bool get isLoadingChats => _isLoadingChats;
  bool get isLoadingMessages => _isLoadingMessages;
  bool get isSending => _isSending;
  String? get errorMessage => _errorMessage;

  List<MessageModel> getMessages(String chatId) => _messages[chatId] ?? [];

  Future<void> fetchChats() async {
    _isLoadingChats = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final response = await _apiService.get(ApiConstants.chats);
      if (response.statusCode == 200) {
        final List<dynamic> list = jsonDecode(response.body);
        _chats = list.map((item) => ChatRoomModel.fromJson(item)).toList();
      }
    } catch (e) {
      _errorMessage = 'Failed to load chats: ${e.toString()}';
    } finally {
      _isLoadingChats = false;
      notifyListeners();
    }
  }

  Future<void> fetchMessages(String chatId, {int page = 0, int size = 50}) async {
    _isLoadingMessages = true;
    notifyListeners();

    try {
      final url = '${ApiConstants.chatMessages(chatId)}?page=$page&size=$size';
      final response = await _apiService.get(url);
      if (response.statusCode == 200) {
        final List<dynamic> list = jsonDecode(response.body);
        final fetchedMessages =
            list.map((item) => MessageModel.fromJson(item)).toList();
        
        // Backend returns messages, let's keep them sorted by createdAt ascending
        fetchedMessages.sort((a, b) => a.createdAt.compareTo(b.createdAt));
        _messages[chatId] = fetchedMessages;
      }
    } catch (e) {
      _errorMessage = 'Failed to load messages: ${e.toString()}';
    } finally {
      _isLoadingMessages = false;
      notifyListeners();
    }
  }

  Future<bool> sendMessage(String chatId, String content) async {
    if (content.trim().isEmpty) return false;

    _isSending = true;
    notifyListeners();

    try {
      final url = ApiConstants.chatMessages(chatId);
      final response = await _apiService.post(
        url,
        body: {
          'messageType': 'TEXT',
          'content': content.trim(),
        },
      );

      if (response.statusCode == 201) {
        final messageData = jsonDecode(response.body);
        final newMsg = MessageModel.fromJson(messageData);
        
        if (_messages[chatId] == null) {
          _messages[chatId] = [];
        }
        _messages[chatId]!.add(newMsg);

        // Update the last message in the chat list
        final index = _chats.indexWhere((c) => c.id == chatId);
        if (index != -1) {
          final old = _chats[index];
          _chats[index] = ChatRoomModel(
            id: old.id,
            type: old.type,
            name: old.name,
            otherUserId: old.otherUserId,
            otherUsername: old.otherUsername,
            lastMessage: LastMessage(
              id: newMsg.id,
              senderUsername: newMsg.senderUsername,
              content: newMsg.content,
              type: newMsg.type,
              createdAt: newMsg.createdAt,
            ),
            unreadCount: old.unreadCount,
            updatedAt: DateTime.now(),
          );
        }

        _isSending = false;
        notifyListeners();
        return true;
      }
    } catch (e) {
      _errorMessage = 'Failed to send message: ${e.toString()}';
    }

    _isSending = false;
    notifyListeners();
    return false;
  }

  Future<ChatRoomModel?> createDirectChat(String recipientId) async {
    try {
      final response = await _apiService.post(
        ApiConstants.directChat,
        body: {'recipientId': recipientId},
      );

      if (response.statusCode == 201) {
        final data = jsonDecode(response.body);
        final newRoom = ChatRoomModel.fromJson(data);
        await fetchChats();
        return newRoom;
      }
    } catch (e) {
      _errorMessage = 'Failed to create chat: ${e.toString()}';
      notifyListeners();
    }
    return null;
  }

  Future<ChatRoomModel?> createGroupChat({
    required String name,
    String? description,
    List<String> memberIds = const [],
  }) async {
    try {
      final response = await _apiService.post(
        ApiConstants.groupChat,
        body: {
          'name': name.trim(),
          'description': description?.trim(),
          'memberIds': memberIds,
        },
      );

      if (response.statusCode == 201) {
        final data = jsonDecode(response.body);
        final newRoom = ChatRoomModel.fromJson(data);
        await fetchChats();
        return newRoom;
      }
    } catch (e) {
      _errorMessage = 'Failed to create group: ${e.toString()}';
      notifyListeners();
    }
    return null;
  }

  Future<void> deleteMessage(String chatId, String messageId) async {
    try {
      final response = await _apiService.delete(ApiConstants.deleteMessage(messageId));
      if (response.statusCode == 204) {
        _messages[chatId]?.removeWhere((m) => m.id == messageId);
        notifyListeners();
      }
    } catch (_) {}
  }
}

