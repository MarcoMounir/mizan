# Mizan — EGX Portfolio Intelligence Platform

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    REACT NATIVE APP                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────────┐  │
│  │ Auth     │ │ Portfolio│ │ Risk     │ │ Market Data   │  │
│  │ Screens  │ │ Screens  │ │ Screens  │ │ (Twelve Data) │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └───────┬───────┘  │
│       └─────────────┴────────────┴───────────────┘           │
│                         │ HTTPS                               │
└─────────────────────────┼────────────────────────────────────┘
                          ▼
┌──────────────────────────────────────────────────────────────┐
│                   SPRING BOOT API                             │
│                                                               │
│  ┌─────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────┐  │
│  │ Auth    │  │ Portfolio│  │ Order    │  │ Market Data │  │
│  │ Ctrl    │  │ Ctrl     │  │ Ctrl     │  │ Ctrl        │  │
│  └────┬────┘  └────┬─────┘  └────┬─────┘  └──────┬──────┘  │
│       │            │             │                │          │
│  ┌────┴────────────┴─────────────┴────────────────┴──────┐  │
│  │              Service Layer                             │  │
│  │  AuthService │ PortfolioService │ OrderService │ etc.  │  │
│  └────┬─────────┬─────────────────┬───────────────┬──────┘  │
│       │         │                 │               │          │
│  ┌────▼──┐ ┌───▼────┐ ┌─────────▼──┐ ┌──────────▼───────┐ │
│  │ JWT   │ │ JPA    │ │ Redis     │ │ RabbitMQ        │ │
│  │ Auth  │ │ Repos  │ │ Cache     │ │ Event Queue     │ │
│  └───────┘ └───┬────┘ └───────────┘ └──────────────────┘ │
└────────────────┼─────────────────────────────────────────────┘
                 ▼
┌──────────────────────────────────────────────────────────────┐
│  PostgreSQL          Redis             RabbitMQ              │
│  ┌────────────┐      ┌───────────┐     ┌──────────────┐     │
│  │ users      │      │ sessions  │     │ price.update │     │
│  │ portfolios │      │ prices    │     │ audit.log    │     │
│  │ orders     │      │ rate-limit│     │ notification │     │
│  │ audit_logs │      └───────────┘     └──────────────┘     │
│  └────────────┘                                              │
└──────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Mobile | React Native + TypeScript |
| Backend API | Spring Boot 3.2 + Java 17 |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Queue | RabbitMQ 3.13 |
| Auth | OAuth2 (Google, Apple) + JWT + Biometric |
| Market Data | Twelve Data API (BYOK per user) |
| Logging | SLF4J + Logback (structured JSON) |
| Migration | Flyway |

## Quick Start

### Prerequisites
- Java 17+, Maven 3.9+
- PostgreSQL 16, Redis 7, RabbitMQ 3.13
- Node.js 18+, React Native CLI
- Docker & Docker Compose (recommended)

### 1. Start infrastructure
```bash
cd backend
docker-compose up -d postgres redis rabbitmq
```

### 2. Start backend
```bash
cd backend
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# Edit application-local.yml with your secrets
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Start mobile
```bash
cd mobile
cp .env.example .env
npm install
npx react-native run-ios   # or run-android
```

## Project Structure

```
mizan-project/
├── backend/                        # Spring Boot API
│   ├── src/main/java/com/mizan/
│   │   ├── config/                 # App, Redis, RabbitMQ, Security config
│   │   ├── controller/             # REST controllers
│   │   ├── dto/                    # Request/Response DTOs
│   │   ├── entity/                 # JPA entities
│   │   ├── enums/                  # AuthProvider, RiskLevel, etc.
│   │   ├── exception/              # Global error handler
│   │   ├── filter/                 # JWT auth filter, rate limit filter
│   │   ├── repository/             # Spring Data JPA repos
│   │   ├── service/                # Business logic
│   │   │   ├── impl/              # Service implementations
│   │   │   └── event/             # RabbitMQ event publishers/listeners
│   │   ├── security/               # JWT provider, OAuth verifiers
│   │   └── util/                   # Helpers
│   ├── src/main/resources/
│   │   ├── application.yml         # Main config
│   │   ├── db/migration/           # Flyway SQL migrations
│   │   └── logback-spring.xml      # Structured JSON logging
│   ├── docker-compose.yml          # Infrastructure
│   └── pom.xml
│
├── mobile/                         # React Native app
│   ├── src/
│   │   ├── api/                    # Axios client, interceptors
│   │   ├── components/             # UI components
│   │   ├── hooks/                  # Custom hooks
│   │   ├── navigation/             # React Navigation config
│   │   ├── screens/                # Screen components
│   │   ├── services/               # Auth, biometric, secure storage
│   │   ├── store/                  # Zustand state management
│   │   ├── theme/                  # Design tokens
│   │   └── types/                  # TypeScript types
│   ├── .env.example
│   └── package.json
│
└── docs/
    └── auth-architecture.md
```
