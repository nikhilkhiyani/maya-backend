# MAYA - E-commerce Backend API

Spring Boot backend for MAYA e-commerce application with JWT authentication, role-based access control, and comprehensive product/order management.

## Tech Stack

- **Java 17**
- **Spring Boot 3.4.5**
- **PostgreSQL** - Database
- **Spring Security** - Authentication & Authorization
- **JWT** - Token-based authentication
- **MapStruct** - DTO mapping
- **Lombok** - Boilerplate reduction
- **Swagger/OpenAPI** - API documentation

## Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6+ (or use included Maven wrapper)

## Database Setup

1. Install PostgreSQL and start the service

2. Create the database:
```sql
CREATE DATABASE MAYA_db;
```

3. Database credentials are configured in `application.properties`:
   - **URL**: `jdbc:postgresql://localhost:5432/MAYA_db`
   - **Username**: `postgres`
   - **Password**: `postgres`

**Note**: Update credentials in `application.properties` if using different values.

## Build & Run

### Using Maven Wrapper (Recommended)

```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

### Using System Maven

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

## API Documentation

Access Swagger UI at: **http://localhost:8080/swagger-ui.html**

All APIs are documented and testable through Swagger.

## API Endpoints

### Authentication (Public)
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

### Products (Public Read, Admin Write)
- `GET /api/products` - Get all products (with filters)
- `GET /api/products/{id}` - Get product by ID
- `POST /api/admin/products` - Create product (Admin)
- `PUT /api/admin/products/{id}` - Update product (Admin)
- `DELETE /api/admin/products/{id}` - Delete product (Admin)

### Orders (Authenticated)
- `POST /api/orders` - Create order
- `GET /api/orders/my` - Get my orders
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/admin/orders` - Get all orders (Admin)
- `PUT /api/admin/orders/{id}/status` - Update order status (Admin)

### Users (Authenticated)
- `GET /api/users/profile` - Get current user profile
- `PUT /api/users/profile` - Update profile
- `GET /api/admin/users` - Get all users (Admin)

### Cart (Authenticated)
- `POST /api/cart` - Add item to cart
- `PUT /api/cart` - Update cart item quantity
- `DELETE /api/cart` - Remove item from cart
- `GET /api/cart` - Get cart items

### Wishlist (Authenticated)
- `POST /api/wishlist` - Add to wishlist
- `DELETE /api/wishlist` - Remove from wishlist
- `GET /api/wishlist` - Get wishlist items

## Authentication

The API uses JWT Bearer token authentication.

### Getting a Token

1. Register or login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

2. Use the returned token in subsequent requests:
```bash
curl -X GET http://localhost:8080/api/orders/my \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## User Roles

- **USER** - Regular customer (can browse, order, manage cart/wishlist)
- **ADMIN** - Administrator (full access to manage products, orders, users)

## Product Filters

The product listing endpoint supports:
- `category` - Filter by category (WOMEN, MEN, JEWELRY, SAREE, LEHENGA, KURTI, ACCESSORIES)
- `search` - Search in name and description
- `minPrice` - Minimum price filter
- `maxPrice` - Maximum price filter

Example:
```
GET /api/products?category=SAREE&minPrice=1000&maxPrice=5000&search=silk
```

## Frontend Integration

### CORS Configuration
Frontend URL is whitelisted: `http://localhost:3000`

### Expected Response Format

**Products:**
```json
{
  "id": "uuid",
  "name": "Product Name",
  "price": 2999.00,
  "discountPrice": 2499.00,
  "images": ["url1", "url2"],
  "category": "SAREE",
  "rating": 4.5
}
```

**Orders:**
```json
{
  "id": "uuid",
  "status": "PLACED",
  "items": [...],
  "totalAmount": 4999.00
}
```

## Creating Admin User

By default, all registered users have the USER role. To create an admin:

1. Register a user normally
2. Manually update the database:
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

## Configuration

Key configurations in `application.properties`:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/MAYA_db
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secret=your-secret-key
jwt.expiration=86400000

# Swagger
springdoc.swagger-ui.path=/swagger-ui.html
```

## Development

### Project Structure
```
src/main/java/com/MAYA/studio/
├── config/          # Security, CORS, Swagger configuration
├── controller/      # REST controllers
├── service/         # Business logic
├── repository/      # Data access layer
├── entity/          # JPA entities
├── dto/             # Data transfer objects
├── mapper/          # MapStruct mappers
├── security/        # JWT utilities, filters
└── exception/       # Exception handling
```

## Troubleshooting

### Database Connection Issues
- Ensure PostgreSQL is running
- Verify database credentials in `application.properties`
- Check if database `MAYA_db` exists

### Build Errors
- Ensure Java 17 is installed: `java -version`
- Clean and rebuild: `./mvnw clean install`

### Port Already in Use
- Change port in `application.properties`: `server.port=8081`

## License

Proprietary - MAYA
