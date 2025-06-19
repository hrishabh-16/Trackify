# 💰 Trackify - Real-Time Expense Tracker

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](https://github.com/hrishabh-16/Trackify)

> **A modern, real-time expense tracking solution built with Spring Boot and Angular, featuring AI-powered categorization, team collaboration, and comprehensive analytics.**

## 🌟 Features

### 💼 Core Expense Management

- ✅ **Real-time CRUD Operations** - Add, edit, delete expenses with instant updates
- 📊 **Smart Categorization** - AI-powered automatic expense categorization
- 📄 **Receipt Management** - Upload and attach digital receipts (PDF, JPG, PNG)
- 📈 **Bulk Import** - CSV/Excel import for easy data migration

### ⚡ Real-Time Functionality

- 🔄 **WebSocket Integration** - Instant updates across all devices
- 📱 **Live Dashboard** - Real-time expense tracking and analytics
- 🔔 **Push Notifications** - Instant alerts for approvals, budget limits
- 🌐 **Cross-Device Sync** - Seamless synchronization across platforms

### 👥 Team Collaboration

- 🏢 **Multi-User Support** - Team and family expense management
- 🔐 **Role-Based Access** - Admin, Editor, Viewer permissions
- ✋ **Approval Workflows** - Submit, review, approve/reject expenses
- 💬 **Commenting System** - Collaborate on expense entries
- 📧 **Email Notifications** - Automated workflow notifications

### 📊 Analytics & Reporting

- 📈 **Interactive Dashboards** - Monthly, category-wise, trend analysis
- 📋 **Custom Reports** - Generate detailed expense reports
- 📤 **Export Options** - PDF, CSV, Excel export capabilities
- 🎯 **Budget Tracking** - Set budgets and receive overspending alerts
- 🔍 **Advanced Filtering** - Filter by date, category, amount, team member

### 🤖 AI-Powered Features

- 🧠 **Smart Categorization** - Automatic expense category suggestions
- 🚨 **Anomaly Detection** - Identify unusual spending patterns
- 📝 **Receipt OCR** - Extract data from receipt images
- 💡 **Spending Insights** - AI-driven financial recommendations

### 🏦 Integrations

- 🏪 **Bank API Integration** - Automatic transaction import
- 📱 **UPI Transaction Processing** - Parse UPI payment messages
- 📲 **SMS Integration** - Extract expenses from transaction SMS
- 🔗 **OAuth2 Authentication** - Google, Facebook login support

## 🛠️ Technology Stack

### Backend

- **Framework**: Spring Boot 3.5.0
- **Language**: Java 21
- **Database**: MySQL 8.0
- **Caching**: Redis
- **Security**: Spring Security + JWT
- **Real-time**: WebSocket
- **Documentation**: OpenAPI/Swagger
- **AI**: Spring AI (OpenAI, Anthropic)
- **File Processing**: Apache POI, iText PDF
- **Testing**: JUnit 5, Testcontainers

### Frontend (Planned)

- **Framework**: Angular 19
- **Language**: TypeScript
- **Styling**: Angular Prime-UI
- **Charts**: Chart.js
- **Real-time**: Socket.io
- **Components**: PrimeNG

### DevOps & Infrastructure

- **Containerization**: Docker
- **Database Migration**: Flyway
- **Build Tool**: Maven
- **Monitoring**: Spring Actuator
- **Logging**: Logback

## 🚀 Quick Start

### Prerequisites

- ☕ Java 21 or higher
- 🐬 MySQL 8.0+
- 📦 Maven 3.6+
- 🐳 Docker
- 🗄️ Redis (optional, for caching)

### 1. Clone the Repository

```bash
git clone https://github.com/hrishabh-16/Trackify.git
cd Trackify
```

### 2. Database Setup

```sql
-- Create database
CREATE DATABASE trackify;

-- Create user (optional)
CREATE USER 'trackify_user'@'localhost' IDENTIFIED BY 'trackify_password';
GRANT ALL PRIVILEGES ON trackify.* TO 'trackify_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configure Application Properties

```properties
# Copy and modify the configuration
cp src/main/resources/application.properties.example src/main/resources/application.properties

# Edit the following values:
spring.datasource.username=root
spring.datasource.password=your_password
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.security.oauth2.client.registration.google.client-id=your-google-client-id
spring.security.oauth2.client.registration.google.client-secret=your-google-client-secret
```

### 4. Run the Application

```bash
# Using Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/trackify-backend-1.0.0.jar
```

### 5. Access the Application

- **API Base URL**: http://localhost:8090/api
- **Swagger UI**: http://localhost:8090/api/swagger-ui.html

## 🐳 Docker Setup

### Using Docker Compose

```bash
# Start all services (MySQL, Redis, Application)
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Using Docker Only

```bash
# Build image
docker build -t trackify-backend .

# Run with environment variables
docker run -p 8090:8090 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/trackify \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=password \
  trackify-backend
```

## 📡 API Documentation

### Authentication Endpoints

```http
POST /api/auth/login          # User login
POST /api/auth/register       # User registration
POST /api/auth/refresh        # Refresh JWT token
POST /api/auth/logout         # User logout
GET  /api/auth/me            # Get current user info
```

### Expense Management

```http
GET    /api/expenses                    # Get all expenses
POST   /api/expenses                    # Create new expense
GET    /api/expenses/{id}               # Get expense by ID
PUT    /api/expenses/{id}               # Update expense
DELETE /api/expenses/{id}               # Delete expense
POST   /api/expenses/bulk-import        # Bulk import expenses
GET    /api/expenses/export             # Export expenses
```

### Team Management

```http
GET    /api/teams                       # Get user teams
POST   /api/teams                       # Create new team
GET    /api/teams/{id}                  # Get team details
PUT    /api/teams/{id}                  # Update team
DELETE /api/teams/{id}                  # Delete team
POST   /api/teams/{id}/invite           # Invite team member
POST   /api/teams/{id}/approve          # Approve expense
```

### Budget & Analytics

```http
GET    /api/budgets                     # Get budgets
POST   /api/budgets                     # Create budget
GET    /api/dashboard                   # Get dashboard data
GET    /api/reports/monthly             # Monthly reports
GET    /api/reports/category            # Category-wise reports
```

### WebSocket Events

```javascript
// Connect to WebSocket
const socket = new WebSocket('ws://localhost:8090/api/ws');

// Listen for real-time updates
socket.onmessage = (event) => {
  const data = JSON.parse(event.data);
  // Handle expense updates, notifications, etc.
};
```

## 🔧 Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=trackify
DB_USERNAME=root
DB_PASSWORD=password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Email
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# JWT
JWT_SECRET=your-super-secret-jwt-key

# AI
OPENAI_API_KEY=your-openai-api-key
ANTHROPIC_API_KEY=your-anthropic-api-key

# File Upload
FILE_UPLOAD_DIR=uploads/
MAX_FILE_SIZE=10MB
```

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

## 📊 Database Schema

### Core Tables

- **users** - User authentication and profile information
- **teams** - Team/organization management
- **team_members** - Team membership and roles
- **expenses** - Main expense records
- **categories** - Expense categories (customizable)
- **budgets** - Budget limits and tracking
- **receipts** - Uploaded receipt files
- **notifications** - System notifications
- **approval_workflows** - Expense approval process
- **audit_logs** - System activity tracking

### Relationships

```sql
users (1) ←→ (n) team_members (n) ←→ (1) teams
users (1) ←→ (n) expenses
teams (1) ←→ (n) expenses
expenses (1) ←→ (n) receipts
expenses (1) ←→ (n) approval_workflows
categories (1) ←→ (n) expenses
budgets (1) ←→ (n) expenses
```

## 🔒 Security Features

### Authentication & Authorization

- **JWT Token-based Authentication**
- **OAuth2 Integration** (Google, Facebook)
- **Role-based Access Control** (ADMIN, USER, VIEWER)
- **CORS Configuration**
- **CSRF Protection**

### Data Security

- **Password Encryption** (BCrypt)

### API Security

- **Input Validation**
- **Request Size Limits**
- **File Type Restrictions**
- **API Versioning**

## 📈 Monitoring & Logging

### Health Checks

- **Application Health**: `/api/actuator/health`
- **Database Status**: `/api/actuator/health/db`
- **Redis Status**: `/api/actuator/health/redis`

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup

```bash
# Fork the repository
git clone https://github.com/your-username/Trackify.git

# Create feature branch
git checkout -b feature/amazing-feature

# Make changes and commit
git commit -m "Add amazing feature"

# Push to branch
git push origin feature/amazing-feature

# Open Pull Request
```

### Code Style

- Follow **Google Java Style Guide**
- Use **meaningful variable names**
- Write **comprehensive tests**
- Add **proper documentation**

### Commit Convention

```bash
feat: add new expense category feature
fix: resolve WebSocket connection issue
docs: update API documentation
test: add integration tests for team management
refactor: improve expense validation logic
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Spring Boot Team** for the amazing framework
- **OpenAI** for AI integration capabilities
- **Chart.js** for beautiful data visualizations
- **MySQL** for reliable data storage
- **Redis** for efficient caching

## 📞 Support

- **Documentation**: [Wiki](https://github.com/hrishabh-16/Trackify/wiki)
- **Issues**: [GitHub Issues](https://github.com/hrishabh-16/Trackify/issues)
- **Discussions**: [GitHub Discussions](https://github.com/hrishabh-16/Trackify/discussions)
- **Email**: hrishabhgautam480@gmail.com

**[⬆ Back to Top](#-trackify---real-time-expense-tracker)**

Made with ❤️ by [Hrishabh](https://github.com/hrishabh-16)
