import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../models/chat_room.dart';
import '../models/contact.dart';
import '../providers/auth_provider.dart';
import '../providers/chat_provider.dart';
import '../providers/contact_provider.dart';
import 'chat_screen.dart';
import 'create_group_screen.dart';
import 'login_screen.dart';
import 'new_chat_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadData();
    });
  }

  Future<void> _loadData() async {
    final chatProvider = Provider.of<ChatProvider>(context, listen: false);
    final contactProvider = Provider.of<ContactProvider>(context, listen: false);
    await Future.wait([
      chatProvider.fetchChats(),
      contactProvider.fetchContacts(),
    ]);
  }

  void _handleLogout() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Log Out'),
        content: const Text('Are you sure you want to log out?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () async {
              Navigator.pop(ctx);
              await Provider.of<AuthProvider>(context, listen: false).logout();
              if (!mounted) return;
              Navigator.pushAndRemoveUntil(
                context,
                MaterialPageRoute(builder: (_) => const LoginScreen()),
                (route) => false,
              );
            },
            child: const Text('Log Out', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final user = auth.currentUser;

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.indigo,
        foregroundColor: Colors.white,
        title: Text(
          _currentIndex == 0
              ? 'Messages'
              : _currentIndex == 1
                  ? 'Contacts'
                  : 'Profile',
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        actions: [
          if (_currentIndex == 0)
            PopupMenuButton<String>(
              icon: const Icon(Icons.add),
              onSelected: (value) {
                if (value == 'direct') {
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const NewChatScreen()),
                  );
                } else if (value == 'group') {
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const CreateGroupScreen()),
                  );
                }
              },
              itemBuilder: (context) => [
                const PopupMenuItem(
                  value: 'direct',
                  child: Row(
                    children: [
                      Icon(Icons.person_add_outlined, color: Colors.indigo),
                      SizedBox(width: 8),
                      Text('New Direct Chat'),
                    ],
                  ),
                ),
                const PopupMenuItem(
                  value: 'group',
                  child: Row(
                    children: [
                      Icon(Icons.group_add_outlined, color: Colors.indigo),
                      SizedBox(width: 8),
                      Text('New Group Chat'),
                    ],
                  ),
                ),
              ],
            ),
          if (_currentIndex == 2)
            IconButton(
              icon: const Icon(Icons.logout),
              tooltip: 'Log Out',
              onPressed: _handleLogout,
            ),
        ],
      ),
      body: IndexedStack(
        index: _currentIndex,
        children: [
          _buildChatsTab(),
          _buildContactsTab(),
          _buildProfileTab(user),
        ],
      ),
      floatingActionButton: _currentIndex == 1
          ? FloatingActionButton(
              backgroundColor: Colors.indigo,
              foregroundColor: Colors.white,
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const NewChatScreen()),
                );
              },
              child: const Icon(Icons.person_add),
            )
          : null,
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.chat_outlined),
            selectedIcon: Icon(Icons.chat),
            label: 'Chats',
          ),
          NavigationDestination(
            icon: Icon(Icons.contacts_outlined),
            selectedIcon: Icon(Icons.contacts),
            label: 'Contacts',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Profile',
          ),
        ],
      ),
    );
  }

  Widget _buildChatsTab() {
    final chatProvider = context.watch<ChatProvider>();
    final chats = chatProvider.chats;

    if (chatProvider.isLoadingChats && chats.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    return RefreshIndicator(
      onRefresh: () => chatProvider.fetchChats(),
      child: chats.isEmpty
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.forum_outlined, size: 64, color: Colors.grey.shade400),
                  const SizedBox(height: 16),
                  Text(
                    'No conversations yet',
                    style: TextStyle(
                      fontSize: 16,
                      color: Colors.grey.shade600,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 8),
                  ElevatedButton.icon(
                    onPressed: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => const NewChatScreen()),
                      );
                    },
                    icon: const Icon(Icons.chat),
                    label: const Text('Start a conversation'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.indigo,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ],
              ),
            )
          : ListView.separated(
              itemCount: chats.length,
              separatorBuilder: (context, index) => const Divider(height: 1, indent: 72),
              itemBuilder: (context, index) {
                final chat = chats[index];
                return _buildChatListItem(chat);
              },
            ),
    );
  }

  Widget _buildChatListItem(ChatRoomModel chat) {
    String subtitle = 'No messages yet';
    String timeStr = '';

    if (chat.lastMessage != null) {
      final lm = chat.lastMessage!;
      if (chat.isGroup && lm.senderUsername != null) {
        subtitle = '${lm.senderUsername}: ${lm.content ?? ''}';
      } else {
        subtitle = lm.content ?? '';
      }
      if (lm.createdAt != null) {
        timeStr = DateFormat('HH:mm').format(lm.createdAt!);
      }
    } else if (chat.updatedAt != null) {
      timeStr = DateFormat('MMM d').format(chat.updatedAt!);
    }

    return ListTile(
      leading: CircleAvatar(
        radius: 24,
        backgroundColor:
            chat.isGroup ? Colors.amber.shade700 : Colors.indigo.shade400,
        child: Icon(
          chat.isGroup ? Icons.group : Icons.person,
          color: Colors.white,
        ),
      ),
      title: Text(
        chat.displayName,
        style: const TextStyle(fontWeight: FontWeight.bold),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Text(
        subtitle,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(color: Colors.grey.shade600),
      ),
      trailing: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          if (timeStr.isNotEmpty)
            Text(
              timeStr,
              style: TextStyle(fontSize: 12, color: Colors.grey.shade500),
            ),
          if (chat.unreadCount > 0)
            Container(
              margin: const EdgeInsets.only(top: 4),
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(
                color: Colors.indigo,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Text(
                '${chat.unreadCount}',
                style: const TextStyle(color: Colors.white, fontSize: 11),
              ),
            ),
        ],
      ),
      onTap: () async {
        await Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => ChatScreen(chatRoom: chat)),
        );
        if (mounted) {
          Provider.of<ChatProvider>(context, listen: false).fetchChats();
        }
      },
    );
  }

  Widget _buildContactsTab() {
    final contactProvider = context.watch<ContactProvider>();
    final contacts = contactProvider.contacts;

    if (contactProvider.isLoading && contacts.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    return RefreshIndicator(
      onRefresh: () => contactProvider.fetchContacts(),
      child: contacts.isEmpty
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.people_outline, size: 64, color: Colors.grey.shade400),
                  const SizedBox(height: 16),
                  Text(
                    'No contacts yet',
                    style: TextStyle(
                      fontSize: 16,
                      color: Colors.grey.shade600,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 8),
                  ElevatedButton.icon(
                    onPressed: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => const NewChatScreen()),
                      );
                    },
                    icon: const Icon(Icons.person_add),
                    label: const Text('Add new contact'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.indigo,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ],
              ),
            )
          : ListView.separated(
              itemCount: contacts.length,
              separatorBuilder: (context, index) => const Divider(height: 1, indent: 72),
              itemBuilder: (context, index) {
                final contact = contacts[index];
                return _buildContactListItem(contact);
              },
            ),
    );
  }

  Widget _buildContactListItem(ContactModel contact) {
    return ListTile(
      leading: CircleAvatar(
        radius: 22,
        backgroundColor: Colors.teal.shade400,
        child: Text(
          contact.displayName.isNotEmpty
              ? contact.displayName[0].toUpperCase()
              : '?',
          style: const TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      title: Text(
        contact.displayName,
        style: const TextStyle(fontWeight: FontWeight.w600),
      ),
      subtitle: Text(
        contact.contactEmail ?? contact.contactPhone ?? contact.contactUsername,
        style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
      ),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          IconButton(
            icon: const Icon(Icons.chat_bubble_outline, color: Colors.indigo),
            tooltip: 'Message',
            onPressed: () async {
              final chatProvider =
                  Provider.of<ChatProvider>(context, listen: false);
              final room =
                  await chatProvider.createDirectChat(contact.contactUserId);
              if (!mounted) return;
              if (room != null) {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => ChatScreen(chatRoom: room)),
                );
              }
            },
          ),
          IconButton(
            icon: const Icon(Icons.delete_outline, color: Colors.grey),
            tooltip: 'Remove',
            onPressed: () {
              Provider.of<ContactProvider>(context, listen: false)
                  .removeContact(contact.contactUserId);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildProfileTab(dynamic user) {
    if (user == null) {
      return const Center(child: Text('No profile loaded'));
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20.0),
      child: Column(
        children: [
          const SizedBox(height: 16),
          CircleAvatar(
            radius: 48,
            backgroundColor: Colors.indigo,
            child: Text(
              user.username.isNotEmpty ? user.username[0].toUpperCase() : '?',
              style: const TextStyle(
                fontSize: 36,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            user.username,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
          ),
          Container(
            margin: const EdgeInsets.only(top: 4),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
            decoration: BoxDecoration(
              color: Colors.indigo.shade50,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              user.role,
              style: TextStyle(
                color: Colors.indigo.shade700,
                fontSize: 12,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          const SizedBox(height: 32),
          Card(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
            elevation: 1,
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.alternate_email, color: Colors.indigo),
                  title: const Text('Email'),
                  subtitle: Text(user.email ?? 'Not set'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.phone_outlined, color: Colors.indigo),
                  title: const Text('Phone'),
                  subtitle: Text(user.phone ?? 'Not set'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.calendar_today_outlined,
                      color: Colors.indigo),
                  title: const Text('Member Since'),
                  subtitle: Text(
                    user.createdAt != null
                        ? DateFormat('MMMM d, yyyy').format(user.createdAt!)
                        : 'Unknown',
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: OutlinedButton.icon(
              onPressed: _handleLogout,
              icon: const Icon(Icons.logout, color: Colors.redAccent),
              label: const Text(
                'Sign Out',
                style: TextStyle(color: Colors.redAccent),
              ),
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: Colors.redAccent),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

