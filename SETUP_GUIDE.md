# Collectibles Marketplace - Setup Guide

## Quick Start

### 1. Database Setup
```bash
mysql -u root -p
CREATE DATABASE collectibles_marketplace;
exit;
mysql -u root -p collectibles_marketplace < database_schema.sql
```

### 2. Backend Setup
```bash
# Update application.yml with your MySQL credentials
# Then run:
mvn clean install
mvn spring-boot:run
```

Backend runs on: http://localhost:8080

### 3. Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

Frontend runs on: http://localhost:5173

## Default Test Accounts

Create test accounts via the registration page or insert directly:

```sql
-- Admin account (password: admin123)
INSERT INTO users (email, username, password_hash, role, is_verified, is_frozen)
VALUES ('admin@test.com', 'admin', '$2a$10$YourBCryptHashHere', 'ADMIN', true, false);

-- Seller account (password: seller123)
INSERT INTO users (email, username, password_hash, role, is_verified, is_frozen)
VALUES ('seller@test.com', 'seller', '$2a$10$YourBCryptHashHere', 'SELLER', true, false);
```

## API Endpoints

- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/api-docs

## Features Implemented

✅ User Authentication (JWT)
✅ User Registration & Login
✅ Browse Marketplace
✅ User Dashboard
✅ Role-Based Access Control
✅ Responsive Design
✅ Modern UI with Tailwind CSS

## Next Steps

To complete the full marketplace:
1. Implement item creation endpoints
2. Add bidding service with Redis locks
3. Implement auction closing scheduler
4. Add WebSocket for live bidding
5. Implement escrow and payment workflows
6. Add admin panel features
7. Implement notification system

## Troubleshooting

**Database Connection Error:**
- Ensure MySQL is running
- Check credentials in application.yml
- Verify database exists

**Frontend Not Loading:**
- Run `npm install` in frontend directory
- Check that backend is running on port 8080
- Clear browser cache

**Build Errors:**
- Ensure Java 17+ is installed
- Run `mvn clean install -U`
