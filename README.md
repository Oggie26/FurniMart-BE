(cd "$(git rev-parse --show-toplevel)" && git apply --3way <<'EOF'
diff --git a/README.md b/README.md
--- a/README.md
+++ b/README.md
@@ -0,0 +1,281 @@
+# E-Commerce Microservices Application
+
+A comprehensive e-commerce platform built with Spring Boot microservices architecture, featuring service discovery, API gateway, message queuing, and multiple specialized services.
+
+## 🏗️ Architecture Overview
+
+This application follows a microservices architecture pattern with the following components:
+
+### Core Services
+
+| Service | Port | Description | Database |
+|---------|------|-------------|----------|
+| **Eureka Server** | 8761 | Service Discovery & Registry | - |
+| **API Gateway** | 8080 | Centralized API Gateway (Spring Cloud Gateway) | - |
+| **User Service** | 8086 | User management, authentication, profiles | PostgreSQL (5435) |
+| **Product Service** | 8084 | Product catalog, inventory management | PostgreSQL (5436) |
+| **Order Service** | 8085 | Order processing, order history | PostgreSQL (5437) |
+| **Inventory Service** | 8083 | Stock management, availability tracking | PostgreSQL (5438) |
+| **Payment Service** | 8088 | Payment processing, transactions | PostgreSQL (5439) |
+| **Delivery Service** | 8089 | Shipping, logistics, tracking | PostgreSQL (5440) |
+| **File Service** | 8090 | File upload, storage (Cloudinary) | PostgreSQL (5441) |
+| **Notification Service** | 8087 | Email notifications, alerts | - |
+
+### Infrastructure Services
+
+| Service | Port | Description |
+|---------|------|-------------|
+| **Redis** | 6379 | Caching layer for user service |
+| **Kafka** | 9092 | Message broker for event-driven communication |
+| **Zookeeper** | 2181 | Kafka coordination service |
+
+## 🚀 Quick Start
+
+### Prerequisites
+
+- Docker and Docker Compose
+- Java 21 (for local development)
+- Maven 3.9+ (for local development)
+
+### Running with Docker Compose
+
+1. **Clone the repository**
+   ```bash
+   git clone <repository-url>
+   cd ecommerce-microservices
+   ```
+
+2. **Start all services**
+   ```bash
+   docker-compose up -d
+   ```
+
+3. **Check service status**
+   ```bash
+   docker-compose ps
+   ```
+
+4. **View logs**
+   ```bash
+   # All services
+   docker-compose logs -f
+   
+   # Specific service
+   docker-compose logs -f user-service
+   ```
+
+### Service URLs
+
+Once all services are running, you can access:
+
+- **Eureka Dashboard**: http://localhost:8761
+- **API Gateway**: http://localhost:8080
+- **Swagger UI** (per service):
+  - User Service: http://localhost:8086/swagger-ui.html
+  - Product Service: http://localhost:8084/swagger-ui.html
+  - Order Service: http://localhost:8085/swagger-ui.html
+  - Inventory Service: http://localhost:8083/swagger-ui.html
+  - Payment Service: http://localhost:8088/swagger-ui.html
+  - Delivery Service: http://localhost:8089/swagger-ui.html
+  - File Service: http://localhost:8090/swagger-ui.html
+  - Notification Service: http://localhost:8087/swagger-ui.html
+
+## 🔧 Configuration
+
+### Environment Variables
+
+The application uses the following environment variables (configured in docker-compose.yml):
+
+- **Database Configuration**:
+  - `POSTGRES_DB`: Database name
+  - `POSTGRES_USER`: Database username
+  - `POSTGRES_PASSWORD`: Database password
+
+- **JWT Configuration**:
+  - `JWT_SECRET`: Secret key for JWT token generation
+  - `JWT_EXPIRATION`: Token expiration time (in milliseconds)
+
+- **Kafka Configuration**:
+  - `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses
+
+- **Redis Configuration**:
+  - `SPRING_DATA_REDIS_HOST`: Redis host
+  - `SPRING_DATA_REDIS_PORT`: Redis port
+
+### Database Ports
+
+| Service | External Port | Internal Port |
+|---------|---------------|---------------|
+| User DB | 5435 | 5432 |
+| Product DB | 5436 | 5432 |
+| Order DB | 5437 | 5432 |
+| Inventory DB | 5438 | 5432 |
+| Payment DB | 5439 | 5432 |
+| Delivery DB | 5440 | 5432 |
+| File DB | 5441 | 5432 |
+
+## 🏛️ Service Details
+
+### User Service
+- **Features**: User registration, authentication, profile management
+- **Technologies**: Spring Security, JWT, Redis caching
+- **Endpoints**: `/user-service/**`
+
+### Product Service
+- **Features**: Product catalog, search, categorization
+- **Technologies**: Spring Data JPA, PostgreSQL
+- **Endpoints**: `/product-service/**`
+
+### Order Service
+- **Features**: Order creation, processing, status tracking
+- **Technologies**: Spring Data JPA, Kafka integration
+- **Endpoints**: `/order-service/**`
+
+### Inventory Service
+- **Features**: Stock management, availability checking
+- **Technologies**: Spring Data JPA, real-time updates
+- **Endpoints**: `/inventory-service/**`
+
+### Payment Service
+- **Features**: Payment processing, transaction management
+- **Technologies**: Spring Data JPA, Kafka integration
+- **Endpoints**: `/payment-service/**`
+
+### Delivery Service
+- **Features**: Shipping management, tracking, logistics
+- **Technologies**: Spring Data JPA, Kafka integration
+- **Endpoints**: `/delivery-service/**`
+
+### File Service
+- **Features**: File upload, storage, management
+- **Technologies**: Cloudinary integration, multipart file handling
+- **Endpoints**: `/file-service/**`
+
+### Notification Service
+- **Features**: Email notifications, alerts
+- **Technologies**: Spring Mail, Thymeleaf templates, Kafka consumers
+- **Endpoints**: `/notification-service/**`
+
+## 🔄 Event-Driven Architecture
+
+The application uses Apache Kafka for event-driven communication:
+
+- **Order Events**: Order creation, status updates
+- **Payment Events**: Payment processing, transaction updates
+- **Inventory Events**: Stock updates, availability changes
+- **Notification Events**: Email triggers, user notifications
+
+## 🛠️ Development
+
+### Local Development Setup
+
+1. **Start infrastructure services**:
+   ```bash
+   docker-compose up -d redis kafka zookeeper
+   ```
+
+2. **Start databases**:
+   ```bash
+   docker-compose up -d user-db product-db order-db inventory-db payment-db delivery-db file-db
+   ```
+
+3. **Run services locally**:
+   ```bash
+   # In each service directory
+   mvn spring-boot:run
+   ```
+
+### Building Individual Services
+
+```bash
+# Build specific service
+cd <service-directory>
+mvn clean package -DskipTests
+
+# Build Docker image
+docker build -t <service-name> .
+```
+
+## 📊 Monitoring & Health Checks
+
+- **Eureka Dashboard**: Service registration and discovery status
+- **Docker Compose**: Container health and resource usage
+- **Application Logs**: Centralized logging via Docker Compose
+
+## 🔒 Security
+
+- **JWT Authentication**: Stateless authentication across services
+- **Service-to-Service Communication**: Internal network isolation
+- **Database Security**: Isolated databases per service
+- **API Gateway**: Centralized security policies
+
+## 🚀 Deployment
+
+### Production Considerations
+
+1. **Environment Variables**: Update all sensitive configuration
+2. **Database Security**: Use strong passwords and SSL connections
+3. **Network Security**: Configure proper firewall rules
+4. **Monitoring**: Implement comprehensive logging and monitoring
+5. **Scaling**: Configure horizontal scaling for high-traffic services
+
+### Scaling Services
+
+```bash
+# Scale specific service
+docker-compose up -d --scale user-service=3
+```
+
+## 📝 API Documentation
+
+Each service provides Swagger UI documentation at:
+- `http://localhost:<service-port>/swagger-ui.html`
+
+## 🐛 Troubleshooting
+
+### Common Issues
+
+1. **Service Discovery Issues**:
+   - Ensure Eureka Server is running first
+   - Check service registration in Eureka dashboard
+
+2. **Database Connection Issues**:
+   - Verify database containers are running
+   - Check database credentials and connection strings
+
+3. **Kafka Connection Issues**:
+   - Ensure Zookeeper is running before Kafka
+   - Check Kafka broker configuration
+
+4. **Port Conflicts**:
+   - Verify no other services are using the configured ports
+   - Update port mappings in docker-compose.yml if needed
+
+### Logs and Debugging
+
+```bash
+# View all logs
+docker-compose logs -f
+
+# View specific service logs
+docker-compose logs -f <service-name>
+
+# View database logs
+docker-compose logs -f <database-name>
+```
+
+## 📄 License
+
+This project is licensed under the MIT License - see the LICENSE file for details.
+
+## 🤝 Contributing
+
+1. Fork the repository
+2. Create a feature branch
+3. Make your changes
+4. Add tests if applicable
+5. Submit a pull request
+
+## 📞 Support
+
+For support and questions, please open an issue in the repository or contact the development team.
EOF
)
