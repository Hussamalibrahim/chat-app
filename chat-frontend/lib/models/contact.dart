class ContactModel {
  final String id;
  final String contactUserId;
  final String contactUsername;
  final String? contactEmail;
  final String? contactPhone;
  final String? alias;
  final DateTime? createdAt;

  ContactModel({
    required this.id,
    required this.contactUserId,
    required this.contactUsername,
    this.contactEmail,
    this.contactPhone,
    this.alias,
    this.createdAt,
  });

  String get displayName =>
      alias?.isNotEmpty == true ? alias! : contactUsername;

  factory ContactModel.fromJson(Map<String, dynamic> json) {
    return ContactModel(
      id: json['id'] as String,
      contactUserId: (json['contactUserId'] ?? json['userId'] ?? '') as String,
      contactUsername:
          (json['contactUsername'] ?? json['username'] ?? 'User') as String,
      contactEmail: (json['contactEmail'] ?? json['email']) as String?,
      contactPhone: (json['contactPhone'] ?? json['phone']) as String?,
      alias: json['alias'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'contactUserId': contactUserId,
      'contactUsername': contactUsername,
      'contactEmail': contactEmail,
      'contactPhone': contactPhone,
      'alias': alias,
      'createdAt': createdAt?.toIso8601String(),
    };
  }
}

