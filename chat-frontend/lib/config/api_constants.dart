import 'package:shared_preferences/shared_preferences.dart';

class ApiConstants {
  static const String defaultLocalhostUrl = 'http://localhost:8080/api/v1';
  static const String defaultLanUrl = 'http://10.234.181.188:8080/api/v1';
  static const String _keyServerUrl = 'custom_server_url';

  static String _serverUrl = defaultLocalhostUrl;

  static Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getString(_keyServerUrl);
    if (saved != null && saved.trim().isNotEmpty) {
      _serverUrl = saved.trim();
    }
  }

  static String get baseUrl => _serverUrl;

  static Future<void> setBaseUrl(String newUrl) async {
    String cleanUrl = newUrl.trim();
    if (cleanUrl.endsWith('/')) {
      cleanUrl = cleanUrl.substring(0, cleanUrl.length - 1);
    }
    if (!cleanUrl.endsWith('/api/v1')) {
      cleanUrl = '$cleanUrl/api/v1';
    }
    _serverUrl = cleanUrl;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyServerUrl, cleanUrl);
  }

  // --- 01. Auth Endpoints ---
  static String get register => '$baseUrl/auth/register';
  static String get login => '$baseUrl/auth/login';
  static String get refresh => '$baseUrl/auth/refresh';
  static String get logout => '$baseUrl/auth/logout';
  static String get logoutAll => '$baseUrl/auth/logout-all';

  // --- 02. User Endpoints ---
  static String get currentUser => '$baseUrl/users/me';
  static String get updateUser => '$baseUrl/users/me';
  static String get updateContact => '$baseUrl/users/me/contact';
  static String get changePassword => '$baseUrl/users/me/password';
  static String get deleteAccount => '$baseUrl/users/me';
  static String get searchUsers => '$baseUrl/users/search';
  static String get findUser => '$baseUrl/users/find';

  // --- 03. Chats & Groups Endpoints ---
  static String get chats => '$baseUrl/chats';
  static String get directChat => '$baseUrl/chats/direct';
  static String get groupChat => '$baseUrl/chats/groups';
  static String chatDetails(String chatId) => '$baseUrl/chats/$chatId';
  static String updateGroup(String groupId) => '$baseUrl/chats/groups/$groupId';
  static String chatMembers(String chatId) => '$baseUrl/chats/$chatId/members';
  static String addMember(String chatId) => '$baseUrl/chats/$chatId/members';
  static String updateMemberRole(String chatId, String userId) =>
      '$baseUrl/chats/$chatId/members/$userId/role';
  static String removeMember(String chatId, String userId) =>
      '$baseUrl/chats/$chatId/members/$userId';
  static String leaveChat(String chatId) => '$baseUrl/chats/$chatId/leave';

  // --- 04. Messages & Attachments Endpoints ---
  static String chatMessages(String chatId) => '$baseUrl/chats/$chatId/messages';
  static String editMessage(String messageId) => '$baseUrl/messages/$messageId';
  static String deleteMessage(String messageId) => '$baseUrl/messages/$messageId';
  static String readMessage(String messageId) => '$baseUrl/messages/$messageId/read';
  static String messageAttachments(String messageId) =>
      '$baseUrl/messages/$messageId/attachments';

  // --- 05. Message Requests Endpoints ---
  static String get messageRequests => '$baseUrl/message-requests';
  static String get pendingMessageRequests => '$baseUrl/message-requests/pending';
  static String acceptMessageRequest(String requestId) =>
      '$baseUrl/message-requests/$requestId/accept';
  static String declineMessageRequest(String requestId) =>
      '$baseUrl/message-requests/$requestId/decline';

  // --- 06. Group Invitations Endpoints ---
  static String inviteGroupMember(String groupId) =>
      '$baseUrl/groups/$groupId/invitations';
  static String get pendingGroupInvitations => '$baseUrl/groups/invitations';
  static String acceptGroupInvitation(String invitationId) =>
      '$baseUrl/groups/invitations/$invitationId/accept';
  static String declineGroupInvitation(String invitationId) =>
      '$baseUrl/groups/invitations/$invitationId/decline';

  // --- 07. Contacts Endpoints ---
  static String get contacts => '$baseUrl/contacts';
  static String get searchContacts => '$baseUrl/contacts/search';
  static String deleteContact(String userId) => '$baseUrl/contacts/$userId';
  static String addContactByPath(String userId) => '$baseUrl/contacts/$userId';

  // --- 08. Privacy & Blocks Endpoints ---
  static String get privacySettings => '$baseUrl/privacy/settings';
  static String get blockedUsers => '$baseUrl/privacy/blocks';
  static String blockUser(String userId) => '$baseUrl/privacy/blocks/$userId';
  static String unblockUser(String userId) => '$baseUrl/privacy/blocks/$userId';

  // --- 09. Reports Endpoints ---
  static String get reports => '$baseUrl/reports';

  // --- 10. Admin Endpoints ---
  static String get adminDashboard => '$baseUrl/admin/dashboard';
  static String get adminStatistics => '$baseUrl/admin/statistics';
  static String get adminUsers => '$baseUrl/admin/users';
  static String get adminSearchUsers => '$baseUrl/admin/users/search';
  static String adminUserDetails(String userId) => '$baseUrl/admin/users/$userId';
  static String adminSuspendUser(String userId, bool suspend) =>
      '$baseUrl/admin/users/$userId/suspend?suspend=$suspend';
  static String get adminReports => '$baseUrl/admin/reports';
  static String adminReportDetails(String reportId) =>
      '$baseUrl/admin/reports/$reportId';
  static String adminReviewReport(String reportId) =>
      '$baseUrl/admin/reports/$reportId/review';
}
