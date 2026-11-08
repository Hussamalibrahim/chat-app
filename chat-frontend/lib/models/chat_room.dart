class LastMessage {
  final String? id;
  final String? senderUsername;
  final String? content;
  final String? type;
  final DateTime? createdAt;

  LastMessage({
    this.id,
    this.senderUsername,
    this.content,
    this.type,
    this.createdAt,
  });

  factory LastMessage.fromJson(Map<String, dynamic> json) {
    return LastMessage(
      id: json['id'] as String?,
      senderUsername: json['senderUsername'] as String?,
      content: json['content'] as String?,
      type: (json['messageType'] ?? json['type']) as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
    );
  }
}

class ChatRoomModel {
  final String id;
  final String type; // DIRECT or GROUP
  final String? name;
  final String? otherUserId;
  final String? otherUsername;
  final LastMessage? lastMessage;
  final int unreadCount;
  final DateTime? updatedAt;

  ChatRoomModel({
    required this.id,
    required this.type,
    this.name,
    this.otherUserId,
    this.otherUsername,
    this.lastMessage,
    this.unreadCount = 0,
    this.updatedAt,
  });

  bool get isGroup => type == 'GROUP';

  String get displayName {
    if (name != null && name!.trim().isNotEmpty) {
      return name!;
    }
    if (isGroup) {
      return 'Group Chat';
    }
    return otherUsername?.isNotEmpty == true ? otherUsername! : 'Direct Message';
  }

  factory ChatRoomModel.fromJson(Map<String, dynamic> json) {
    return ChatRoomModel(
      id: json['id'] as String,
      type: json['type'] as String? ?? 'DIRECT',
      name: json['name'] as String?,
      otherUserId: json['otherUserId'] as String?,
      otherUsername: json['otherUsername'] as String?,
      lastMessage: json['lastMessage'] != null
          ? LastMessage.fromJson(json['lastMessage'] as Map<String, dynamic>)
          : null,
      unreadCount: json['unreadCount'] as int? ?? 0,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'] as String)
          : null,
    );
  }
}
