class UserModel {
  final String id;
  final String username;
  final String? email;
  final String? phone;
  final String role;
  final DateTime? createdAt;

  UserModel({
    required this.id,
    required this.username,
    this.email,
    this.phone,
    required this.role,
    this.createdAt,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as String,
      username: json['username'] as String,
      email: json['email'] as String?,
      phone: json['phone'] as String?,
      role: json['role'] as String? ?? 'USER',
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'username': username,
      'email': email,
      'phone': phone,
      'role': role,
      'createdAt': createdAt?.toIso8601String(),
    };
  }
}
