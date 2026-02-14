# Environment Setup Guide

## Required Environment Variables

Before running the application, you must configure the following environment variables. Copy `.env.example` to `.env` and fill in your actual values.

### Database Configuration

```env
DB_URL=jdbc:mysql://localhost:3306/collectibles_marketplace?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password
```

**Setup Steps:**
1. Install MySQL 8.0+
2. Create database: `CREATE DATABASE collectibles_marketplace;`
3. Set your MySQL username and password in `.env`

### JWT Secret

```env
JWT_SECRET=your-256-bit-secret-key
```

**Generate a secure key:**
```bash
openssl rand -base64 64
```

### Mail Configuration

For email notifications, configure an SMTP server (Mailtrap recommended for development):

```env
MAIL_HOST=smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=your_mailtrap_username
MAIL_PASSWORD=your_mailtrap_password
```

**Mailtrap Setup:**
1. Sign up at [mailtrap.io](https://mailtrap.io)
2. Create an inbox
3. Copy SMTP credentials to `.env`

### Redis Configuration

```env
REDIS_HOST=localhost
REDIS_PORT=6379
```

**Installation:**
```bash
# macOS
brew install redis
brew services start redis

# Ubuntu/Debian
sudo apt-get install redis-server
sudo systemctl start redis

# Docker
docker run -d --name redis -p 6379:6379 redis:latest
```

### RabbitMQ Configuration

```env
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

**Installation:**
```bash
# macOS
brew install rabbitmq
brew services start rabbitmq

# Ubuntu/Debian
sudo apt-get install rabbitmq-server
sudo systemctl start rabbitmq-server

# Docker
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

## Optional Configuration

### CORS Origins

```env
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

Add additional origins separated by commas.

### Logging Level

```env
LOGGING_LEVEL=INFO
```

Options: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`

## Verification

After configuring all environment variables:

1. **Test Database Connection:**
   ```bash
   mysql -u your_username -p -e "SHOW DATABASES;"
   ```

2. **Test Redis:**
   ```bash
   redis-cli ping
   # Should return: PONG
   ```

3. **Test RabbitMQ:**
   ```bash
   rabbitmqctl status
   # Or visit: http://localhost:15672 (guest/guest)
   ```

4. **Run Application:**
   ```bash
   mvn spring-boot:run
   ```

## Production Deployment

For production environments:

1. **Use strong passwords** for all services
2. **Enable SSL/TLS** for database connections
3. **Use environment-specific** `.env` files
4. **Never commit** `.env` files to version control
5. **Rotate JWT secrets** regularly
6. **Use managed services** (AWS RDS, ElastiCache, Amazon MQ) when possible

## Troubleshooting

### "Could not connect to database"
- Verify MySQL is running
- Check DB_URL, DB_USERNAME, DB_PASSWORD
- Ensure database exists

### "JWT secret cannot be null"
- Ensure JWT_SECRET is set in `.env`
- Verify `.env` file is in the project root

### "Connection refused to Redis"
- Start Redis: `redis-server`
- Check REDIS_HOST and REDIS_PORT

### "RabbitMQ connection failed"
- Start RabbitMQ service
- Verify credentials and port (5672)
