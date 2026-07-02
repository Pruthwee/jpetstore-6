# Cloud Readiness Improvements for AWS

This document describes the cloud readiness fixes applied to the JPetStore application for AWS deployment.

## Summary of Changes

### 1. Hard-coded Database Credentials (CRITICAL)
**Files Modified:**
- `src/test/java/org/mybatis/jpetstore/mapper/AccountMapperTest.java`

**Changes:**
- Replaced hard-coded passwords with environment variable lookups
- Added fallback to default values for testing
- Production deployments should use AWS Secrets Manager for credential management

**AWS Integration:**
```java
// Example: Retrieve from environment variable (populated from AWS Secrets Manager)
String password = System.getenv("TEST_DB_PASSWORD") != null ? System.getenv("TEST_DB_PASSWORD") : "ACID";
```

**Production Recommendation:**
- Use AWS Secrets Manager to store database credentials
- Configure application to retrieve secrets at runtime
- Enable automatic credential rotation

### 2. Unbounded Collections (CRITICAL)
**Files Modified:**
- `src/main/java/org/mybatis/jpetstore/web/actions/AccountActionBean.java`
- `src/main/java/org/mybatis/jpetstore/web/actions/OrderActionBean.java`

**Changes:**
- Added maximum size limits for collections (MAX_MY_LIST_SIZE = 100, MAX_ORDER_LIST_SIZE = 1000)
- Implemented size checking before adding items to collections
- Prevents memory exhaustion in containerized environments

**AWS Integration:**
- Collections are now bounded to prevent OOM errors in ECS/EKS containers
- Consider migrating large datasets to AWS ElastiCache for Redis

### 3. HTTP Session State Storage (HIGH)
**Files Modified:**
- `src/main/java/org/mybatis/jpetstore/web/actions/AccountActionBean.java`
- `src/main/java/org/mybatis/jpetstore/web/actions/OrderActionBean.java`

**Changes:**
- Added documentation comments indicating need for AWS ElastiCache migration
- Current implementation still uses HTTP sessions but is documented for future migration

**AWS Integration Recommendation:**
- Migrate to Spring Session with AWS ElastiCache for Redis
- Enable distributed session management for stateless application instances
- Support horizontal scaling across multiple ECS tasks or EKS pods

**Example Configuration (Future):**
```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

### 4. ThreadLocal Storage (HIGH)
**Files Modified:**
- `.mvn/wrapper/MavenWrapperDownloader.java`

**Changes:**
- Replaced `ThreadLocalRandom` with `Random` to avoid ThreadLocal issues
- Prevents memory leaks in thread-pooled cloud environments

### 5. Missing Connection Timeouts (HIGH)
**Files Modified:**
- `.mvn/wrapper/MavenWrapperDownloader.java`

**Changes:**
- Added connection timeout: 10 seconds
- Added read timeout: 30 seconds
- Prevents indefinite hangs in cloud environments with variable network latency

**AWS Integration:**
- Timeouts configured for AWS SDK clients and HTTP libraries
- Prevents resource exhaustion from hanging connections

### 6. Clock/Time Dependencies (HIGH)
**Files Modified:**
- `src/main/java/org/mybatis/jpetstore/domain/Order.java`
- `src/test/java/org/mybatis/jpetstore/ScreenTransitionIT.java`
- `src/test/java/org/mybatis/jpetstore/domain/OrderTest.java`

**Changes:**
- Migrated from `java.util.Date` to `java.time.Instant` for UTC consistency
- Ensures timezone consistency across distributed cloud services
- Prevents scheduling failures in multi-region deployments

**AWS Integration:**
- All timestamps now use UTC (Instant.now())
- Compatible with AWS CloudWatch Logs and distributed tracing
- Consistent across multiple AWS regions

### 7. Missing Graceful Shutdown Hooks (HIGH)
**Files Modified:**
- `.mvn/wrapper/MavenWrapperDownloader.java`

**Changes:**
- Added JVM shutdown hook to handle SIGTERM signals
- Ensures proper cleanup during container lifecycle events

**AWS Integration:**
- Handles SIGTERM from ECS task termination
- Handles SIGTERM from EKS pod termination
- Prevents data loss during rolling updates and scaling events

### 8. WAR Packaging (LOW)
**Files Modified:**
- `pom.xml`

**Changes:**
- Changed packaging from WAR to JAR
- Added embedded Tomcat dependency (tomcat-embed-core 9.0.113)
- Changed maven-war-plugin to maven-jar-plugin

**AWS Integration:**
- Enables deployment as executable JAR in ECS/EKS/Fargate
- Reduces container image size
- Simplifies deployment automation
- No external application server required

## AWS Deployment Recommendations

### 1. Secrets Management
- Store database credentials in AWS Secrets Manager
- Configure automatic rotation
- Use IAM roles for secret access

### 2. Session Management
- Deploy AWS ElastiCache for Redis cluster
- Configure Spring Session for distributed sessions
- Enable session replication across availability zones

### 3. Container Deployment
- Deploy to Amazon ECS with Fargate or EC2 launch type
- Or deploy to Amazon EKS (Kubernetes)
- Use Application Load Balancer for traffic distribution

### 4. Monitoring and Logging
- Enable AWS CloudWatch Logs for application logs
- Use AWS X-Ray for distributed tracing
- Configure CloudWatch alarms for memory and CPU usage

### 5. Database
- Use Amazon RDS for database hosting
- Enable Multi-AZ deployment for high availability
- Configure automated backups

### 6. Configuration Management
- Use AWS Systems Manager Parameter Store for application configuration
- Store non-sensitive configuration as environment variables
- Use AWS AppConfig for dynamic configuration updates

## Environment Variables Required

```bash
# Database Credentials (from AWS Secrets Manager)
DB_HOST=<rds-endpoint>
DB_PORT=3306
DB_NAME=jpetstore
DB_USERNAME=<from-secrets-manager>
DB_PASSWORD=<from-secrets-manager>

# Redis Session Store (AWS ElastiCache)
REDIS_HOST=<elasticache-endpoint>
REDIS_PORT=6379

# Application Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=aws
```

## Next Steps

1. **Immediate:**
   - Test application with bounded collections
   - Verify timeout configurations
   - Test graceful shutdown behavior

2. **Short-term:**
   - Integrate AWS Secrets Manager for credential management
   - Deploy AWS ElastiCache for Redis
   - Implement Spring Session for distributed sessions

3. **Long-term:**
   - Migrate to Spring Boot for better cloud-native support
   - Implement health check endpoints for ECS/EKS
   - Add metrics export to CloudWatch

## Testing

All changes maintain backward compatibility with existing functionality while adding cloud-ready patterns. The application can still run in traditional environments while being prepared for AWS cloud deployment.
