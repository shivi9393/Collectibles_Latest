# Collectibles Auction Marketplace

A full-stack auction platform for collectibles with real-time bidding, secure escrow payments, and event-driven notifications.

## Features

### Core Functionality
- **User Authentication & Authorization**: JWT-based authentication with role-based access control
- **Real-time Bidding System**: Live auction bidding with automatic bid increments and proxy bidding
- **Escrow Payment System**: Secure double-entry ledger for all transactions
- **Shipping & Logistics**: Order tracking with automated escrow release
- **Event-Driven Notifications**: Asynchronous email and WebSocket notifications via RabbitMQ

### Technical Highlights
- **Concurrency Control**: Redis-based distributed locking for bid processing
- **Automated Schedulers**: Auction closing and escrow auto-release
- **File Upload**: Image management for collectible listings
- **RESTful API**: Comprehensive API with Swagger documentation

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.2.1
- **Database**: MySQL 8.0
- **Cache**: Redis
- **Message Broker**: RabbitMQ
- **Security**: Spring Security + JWT
- **ORM**: Hibernate/JPA

### Frontend
- **Framework**: React 18
- **Routing**: React Router
- **HTTP Client**: Axios
- **Styling**: CSS3

## Prerequisites

- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.12+
- Node.js 16+ (for frontend)

## Getting Started

### 1. Clone the Repository
```bash
git clone <repository-url>
cd Collectibles
```

### 2. Configure Environment Variables

Copy the example environment file and configure your credentials:

```bash
cp .env.example .env
```

Edit `.env` with your actual configuration:

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/collectibles_marketplace?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# JWT Secret (generate a secure 256-bit key)
JWT_SECRET=your-secure-jwt-secret-key

# Mail Configuration (for notifications)
MAIL_HOST=smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=your_mailtrap_username
MAIL_PASSWORD=your_mailtrap_password

# RabbitMQ (if not using defaults)
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

### 3. Database Setup

Create the MySQL database:

```sql
CREATE DATABASE collectibles_marketplace;
```

The application will automatically create tables on first run using Hibernate's `ddl-auto: update`.

Optionally, you can run the provided schema:

```bash
mysql -u your_username -p collectibles_marketplace < database_schema.sql
```

### 4. Start Required Services

**Redis:**
```bash
redis-server
```

**RabbitMQ:**
```bash
# macOS (via Homebrew)
brew services start rabbitmq

# Linux
sudo systemctl start rabbitmq-server

# Docker
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### 5. Run the Backend

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`.

### 6. Run the Frontend

```bash
cd frontend
npm install
npm start
```

The frontend will start on `http://localhost:3000`.

## API Documentation

Once the application is running, access the Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

## Project Structure

```
Collectibles/
├── src/main/java/com/marketplace/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── entity/          # JPA entities
│   ├── repository/      # Data repositories
│   ├── service/         # Business logic
│   ├── scheduler/       # Scheduled tasks
│   ├── event/           # Domain events
│   └── consumer/        # Message consumers
├── src/main/resources/
│   └── application.yml  # Application configuration
├── frontend/            # React frontend
└── pom.xml             # Maven dependencies
```

## Key Workflows

### Auction Flow
1. Seller creates an item listing
2. Auction starts automatically
3. Buyers place bids (with concurrency control)
4. Scheduler closes auction at end time
5. Winner is notified and order is created

### Payment & Escrow
1. Buyer pays for won auction
2. Funds held in escrow
3. Seller ships item
4. Buyer confirms delivery (or auto-confirms after 7 days)
5. Escrow releases funds to seller

### Notifications
1. Services publish domain events
2. RabbitMQ routes events to consumers
3. NotificationService sends emails and WebSocket messages
4. Notifications persisted in database

## Testing

Run all tests:

```bash
mvn test
```

Run specific test class:

```bash
mvn -Dtest=NotificationIntegrationTest test
```

## Environment Variables Reference

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | MySQL connection URL | `jdbc:mysql://localhost:3306/collectibles_marketplace...` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | *(required)* |
| `JWT_SECRET` | JWT signing key (256-bit) | *(required)* |
| `MAIL_HOST` | SMTP server host | `smtp.mailtrap.io` |
| `MAIL_PORT` | SMTP server port | `2525` |
| `MAIL_USERNAME` | SMTP username | *(required)* |
| `MAIL_PASSWORD` | SMTP password | *(required)* |
| `REDIS_HOST` | Redis server host | `localhost` |
| `REDIS_PORT` | Redis server port | `6379` |
| `RABBITMQ_HOST` | RabbitMQ server host | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ server port | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ username | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `guest` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:3000,http://localhost:5173` |
| `LOGGING_LEVEL` | Application logging level | `INFO` |

## Security Notes

- **Never commit `.env` files** to version control
- Generate a strong JWT secret using: `openssl rand -base64 64`
- Use environment-specific configurations for production
- Enable SSL/TLS for production databases
- Configure proper CORS origins for production

## Troubleshooting

### Database Connection Issues
- Verify MySQL is running: `mysql -u root -p`
- Check credentials in `.env`
- Ensure database exists

### RabbitMQ Connection Failed
- Verify RabbitMQ is running: `rabbitmqctl status`
- Check management console: `http://localhost:15672`

### Redis Connection Issues
- Verify Redis is running: `redis-cli ping`
- Should return `PONG`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## Support

For issues and questions, please open an issue on GitHub.
