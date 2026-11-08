# Chat Application Backend (Reactive Real-Time Messaging)

A reactive, scalable real-time messaging backend built with **Java 21**, **Spring Boot**, **Spring WebFlux**, **Spring Security**, **Spring Data R2DBC**, **PostgreSQL 18**, **Apache Kafka**, and **Grafana / Prometheus**.

---

## 1. System Architecture

The application adopts a clean, practical **Layered Architecture** with direct reactive dependencies:

$$\text{Controller} \longrightarrow \text{Service} \longrightarrow \text{Repository} \longrightarrow \text{PostgreSQL (R2DBC)}$$

* **Non-blocking Reactive I/O**: Completely asynchronous request and database pipelining using Project Reactor (`Mono` and `Flux`).
* **Thin Controllers**: Controllers handle HTTP route binding, validation, and user identity extraction from `Authentication`.
* **Rich Domain Services**: Business logic, privacy policies, group role permissions, and validation reside in services.
* **Spring Data R2DBC Repositories**: Pure reactive SQL database interactions without JPA or blocking JDBC.
* **Transactional Outbox Pattern**: Message events are persisted in PostgreSQL (`outbox_events`) in the same unit of work as domain data, and dispatched to Kafka with zero message loss.

### Package Structure

```
com.chatapp.chat_backend
├── config                     # App, Security, Kafka, R2DBC, and Prometheus Metrics configuration
├── controller                 # Reactive WebFlux Controllers
│   ├── admin                  # Dashboard statistics, user management, and report moderation
│   ├── auth                   # Registration, login, token refresh, and logout
│   ├── chat                   # Direct chats, group chats, room members, and role assignments
│   ├── contact                # Contact management and contact search
│   ├── group                  # Group invitations workflow
│   ├── message                # Sending, updating, reading, deleting messages, and message requests
│   ├── privacy                # User privacy settings and user blocking
│   ├── report                 # User and message reporting
│   └── user                   # User profile, search, update, password change, soft-delete
├── dto
│   ├── event                  # Kafka event payloads (ChatMessageEvent, NotificationEvent)
│   ├── request                # Input request records with validation constraints
│   └── response               # Strongly-typed API responses
├── exception                  # Global WebFlux Exception Handler and Error Codes
├── model                      # Reactive entities implementing Persistable<UUID>
├── repository                 # Spring Data R2DBC reactive repositories
├── security                   # JWT filter, ReactiveAuthenticationManager, TokenService
└── service                    # Domain business logic and orchestrations
```

---

## 2. Database Schema (PostgreSQL 18)

The schema defines **16 core tables** plus the **Outbox Events** table:

1. `users`: Credentials, role (`USER`, `ADMIN`), contact info, soft-deletion (`deleted_at`).
2. `chat_rooms`: Common container for direct and group conversations.
3. `direct_chats`: 1-on-1 conversations with a unique user-pair constraint (`user1_id < user2_id`).
4. `group_chats`: Group conversations, names, and creators.
5. `chat_room_members`: Membership and role hierarchy (`OWNER`, `ADMIN`, `MEMBER`).
6. `messages`: Text, file, or image messages; soft deletion (`deleted_at`).
7. `message_requests`: Direct messaging requests between non-contacts.
8. `contacts`: User contact lists with self-addition checks.
9. `group_invitations`: Group invitation lifecycle (`PENDING`, `ACCEPTED`, `DECLINED`).
10. `user_privacy_settings`: Privacy preferences (`who_can_message`, `who_can_add_to_groups`).
11. `user_blocks`: Bidirectional interaction block enforcement.
12. `message_attachments`: File metadata and storage references.
13. `message_reads`: Per-user message read tracking.
14. `refresh_tokens`: Secure SHA-256 hashed refresh tokens with rotation and revocation.
15. `user_reports`: User and message abuse reports.
16. `user_report_reviews`: Administrative moderation resolutions and audit trail.
17. `outbox_events`: Guaranteed event delivery to Kafka.

All schema tables and performance indexes are automatically loaded from `src/main/resources/schema.sql`.

---

## 3. Apache Kafka Event-Driven Architecture

Kafka is used for asynchronous event distribution, notifications, and analytics:

* **Strict Ordering**: Messages are partitioned by `chatRoomId` as the Kafka message key, guaranteeing strict in-order processing per conversation.
* **Zero Message Loss (Transactional Outbox)**: Every message or notification event is saved to `outbox_events` with status `PENDING`. On successful broker publish, status transitions to `PUBLISHED`.
* **Outbox Poller Relay**: `OutboxRelayService` runs every 5 seconds, picking up any pending outbox records and relaying them to Kafka if temporary network or broker outages occur.
* **Topics**:
  - `chat.message.created`
  - `chat.message.deleted`
  - `chat.message.read`
  - `chat.notification.requested`

---

## 4. Monitoring & Observability (Grafana & Prometheus)

* **Micrometer Prometheus Actuator**: Available at `/actuator/prometheus`.
* **Cached Business Metrics**: `BusinessMetricsService` refreshes metrics every 60s to prevent expensive database queries during Prometheus scrapes:
  - `chat_users_total`
  - `chat_users_active_24h`
  - `chat_messages_total`
  - `chat_chat_rooms_total`
  - `chat_group_chats_total`
  - `chat_direct_chats_total`
  - `chat_reports_total`
  - `chat_reports_pending`
* **Grafana Dashboard**: Auto-provisioned in `grafana/provisioning/dashboards/chat-dashboard.json`.

---

## 5. Quickstart Guide

### Prerequisites
* Java 21+
* Docker & Docker Compose

### 1. Launch Infrastructure
Start PostgreSQL, Apache Kafka (KRaft), Prometheus, and Grafana:
```powershell
docker compose up -d
```

| Service | Port | URL / Credentials |
| :--- | :--- | :--- |
| **PostgreSQL** | `5432` | `localhost:5432/chat_db` (`postgres` / `2005`) |
| **Kafka (KRaft)** | `9092` (Host), `29092` (Docker) | `localhost:9092` |
| **Prometheus** | `9090` | [http://localhost:9090](http://localhost:9090) |
| **Grafana** | `3000` | [http://localhost:3000](http://localhost:3000) (`admin` / `admin`) |

### 2. Run the Application
```powershell
.\mvnw.cmd spring-boot:run
```
The application will start on port `8080`.

### 3. Run Unit Tests
```powershell
.\mvnw.cmd test
```
