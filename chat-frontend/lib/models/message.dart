class MessageModel {
  final String id;
  final String chatRoomId;
  final String senderId;
  final String senderUsername;
  final String type;
  final String content;
  final bool isDeleted;
  final DateTime createdAt;
  final int attachmentCount;

  MessageModel({
    required this.id,
    required this.chatRoomId,
    required this.senderId,
    required this.senderUsername,
    required this.type,
    required this.content,
    required this.isDeleted,
    required this.createdAt,
    this.attachmentCount = 0,
  });

  factory MessageModel.fromJson(Map<String, dynamic> json) {
    return MessageModel(
      id: json['id'] as String,
      chatRoomId: (json['chatRoomId'] ?? '') as String,
      senderId: (json['senderId'] ?? '') as String,
      senderUsername: (json['senderUsername'] ?? 'User') as String,
      type: (json['messageType'] ?? json['type'] ?? 'TEXT') as String,
      content: (json['content'] ?? '') as String,
      isDeleted: json['deletedAt'] != null || (json['isDeleted'] as bool? ?? false),
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String) ?? DateTime.now()
          : DateTime.now(),
      attachmentCount: json['attachmentCount'] as int? ?? 0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'chatRoomId': chatRoomId,
      'senderId': senderId,
      'senderUsername': senderUsername,
      'type': type,
      'content': content,
      'isDeleted': isDeleted,
      'createdAt': createdAt.toIso8601String(),
      'attachmentCount': attachmentCount,
    };
  }
}
