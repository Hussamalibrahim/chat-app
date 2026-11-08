import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user.dart';
import '../providers/auth_provider.dart';
import '../providers/chat_provider.dart';
import '../providers/contact_provider.dart';
import 'chat_screen.dart';

class NewChatScreen extends StatefulWidget {
  const NewChatScreen({super.key});

  @override
  State<NewChatScreen> createState() => _NewChatScreenState();
}

class _NewChatScreenState extends State<NewChatScreen> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _startChat(UserModel user) async {
    final chatProvider = Provider.of<ChatProvider>(context, listen: false);
    final room = await chatProvider.createDirectChat(user.id);
    if (!mounted) return;

    if (room != null) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => ChatScreen(chatRoom: room)),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(chatProvider.errorMessage ?? 'Failed to open chat'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  Future<void> _addContact(UserModel user) async {
    final contactProvider = Provider.of<ContactProvider>(context, listen: false);
    final success = await contactProvider.addContact(user.id);
    if (!mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          success
              ? 'Added ${user.username} to contacts'
              : (contactProvider.errorMessage ?? 'Failed to add contact'),
        ),
        backgroundColor: success ? Colors.green : Colors.redAccent,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final contactProvider = context.watch<ContactProvider>();
    final currentUserId =
        context.watch<AuthProvider>().currentUser?.id;

    final results = contactProvider.searchedUsers
        .where((u) => u.id != currentUserId)
        .toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Start New Chat'),
        backgroundColor: Colors.indigo,
        foregroundColor: Colors.white,
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: 'Search user by username or email...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          _searchController.clear();
                          contactProvider.clearSearch();
                        },
                      )
                    : null,
                filled: true,
                fillColor: Colors.grey.shade100,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
              ),
              onChanged: (query) {
                contactProvider.searchUsers(query);
              },
            ),
          ),
          Expanded(
            child: contactProvider.isLoading
                ? const Center(child: CircularProgressIndicator())
                : results.isEmpty
                    ? Center(
                        child: Text(
                          _searchController.text.isEmpty
                              ? 'Type a username or email to search'
                              : 'No users found',
                          style: const TextStyle(color: Colors.grey),
                        ),
                      )
                    : ListView.builder(
                        itemCount: results.length,
                        itemBuilder: (context, index) {
                          final user = results[index];
                          return ListTile(
                            leading: CircleAvatar(
                              backgroundColor: Colors.indigo.shade100,
                              child: Text(
                                user.username.isNotEmpty
                                    ? user.username[0].toUpperCase()
                                    : '?',
                                style: TextStyle(
                                  color: Colors.indigo.shade900,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                            title: Text(
                              user.username,
                              style: const TextStyle(fontWeight: FontWeight.w600),
                            ),
                            subtitle: Text(user.email ?? user.phone ?? ''),
                            trailing: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                IconButton(
                                  icon: const Icon(
                                    Icons.person_add_alt_1,
                                    color: Colors.teal,
                                  ),
                                  tooltip: 'Add Contact',
                                  onPressed: () => _addContact(user),
                                ),
                                IconButton(
                                  icon: const Icon(
                                    Icons.chat,
                                    color: Colors.indigo,
                                  ),
                                  tooltip: 'Direct Message',
                                  onPressed: () => _startChat(user),
                                ),
                              ],
                            ),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}

