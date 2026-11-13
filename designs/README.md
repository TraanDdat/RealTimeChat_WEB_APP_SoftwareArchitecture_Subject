# Overall Real Time Chat Architecture
                         ┌────────────────────┐
                         │       User         │
                         └─────────┬──────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
              ┌─────▼─────┐                 ┌────▼─────┐
              │ Android   │                 │   Web    │
              │   App     │                 │   App    │
              └─────┬─────┘                 └────┬─────┘
                    │                            │
                    └────────────┬───────────────┘
                                 │
                       HTTPS / WebSocket
                                 │
                    ┌────────────▼────────────┐
                    │      API Gateway        │
                    └────────────┬────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
        ┌─────▼─────┐      ┌─────▼─────┐      ┌────▼─────┐
        │   Auth    │      │   Chat     │      │  User    │
        │  Service  │      │  Service   │      │ Service  │
        └─────┬─────┘      └─────┬─────┘      └────┬─────┘
              │                  │                  │
              └──────────────────┼──────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │       Database          │
                    │       PostgreSQL        │
                    └─────────────────────────┘

                    ┌─────────────────────────┐
                    │ Redis / Message Broker   │
                    │     WebSocket / Events   │
                    └─────────────────────────┘

                    ┌─────────────────────────┐
                    │ Push Notification       │
                    │ FCM                     │
                    └─────────────────────────┘
# C4 Architecture
## Level 1 - System Context
                 ┌─────────────┐
                 │    User     │
                 └──────┬──────┘
                        │
                        │ uses
                        ▼
              ┌──────────────────┐
              │   RealTimeChat   │
              │      System      │
              └──────────────────┘
                  │       │
          ┌───────┘       └────────┐
          ▼                        ▼
    Authentication             Firebase
       Service                Notification
## Level 2 - Container Diagram
                    RealTimeChat System
                           │
        ┌──────────────────┼─────────────────┐
        │                  │                 │
        ▼                  ▼                 ▼
   Web Frontend      Android Frontend    Backend API
                                           │
                              ┌────────────┼────────────┐
                              │            │            │
                              ▼            ▼            ▼
                           Auth         Chat        Notification
                           Module       Module         Module
                              │            │
                              └─────┬──────┘
                                    ▼
                               PostgreSQL
                                    │
                                  Redis

# Complete Flow Based On Diagram
             COMPONENT
                 │
                 │ defines architecture
                 ▼
       ┌─────────────────────┐
       │                     │
       │  Web / Android      │
       │         │           │
       │         ▼           │
       │      Backend        │
       │         │           │
       │         ▼           │
       │     PostgreSQL      │
       │                     │
       └─────────────────────┘
                 │
                 │
                 ▼
             SEQUENCE
                 │
                 │ describes runtime behavior
                 ▼
       User A → Server → DB
                  │
                  ▼
                User B
                 │
                 ▼
               CLASS
                 │
                 │ describes data/domain
                 ▼
       User / Conversation
              / Message
## Abstract Diagram
01. Use Case Diagram
       │
       ├── Login
       ├── Send Message
       ├── Create Group
       ├── Manage Profile
       └── Receive Notification

02. Component Diagram
       │
       ├── Web
       ├── Android
       ├── Backend
       ├── Database
       ├── Redis
       └── FCM

03. Class Diagram
       │
       ├── User
       ├── Conversation
       ├── Message
       ├── Attachment
       └── Notification

04. Sequence Diagram
       │
       ├── Login
       ├── Send Message
       ├── Read Message
       └── Create Group

05. Deployment Diagram
       │
       ├── Web Server
       ├── Application Server
       ├── PostgreSQL
       ├── Redis
       └── FCM