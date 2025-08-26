# Database Setup Guide - MSMQ Manager

## Overview
This document describes the database setup and migration process for the MSMQ Manager application using **PostgreSQL** and **Flyway** for database schema management.

## Database Configuration

### Connection Details
- **Host**: localhost
- **Port**: 5432
- **Database**: msmq_manager
- **Username**: postgres
- **Password**: Approved12@

### Environment-Specific Databases
- **Development**: `msmq_manager`
- **Testing**: `msmq_manager_test`
- **Staging**: `msmq_manager` (on staging server)
- **Production**: `msmq_manager` (on production server)

## Prerequisites

### 1. PostgreSQL Installation
Ensure PostgreSQL is installed and running on your system:
```bash
# Check if PostgreSQL is running
pg_isready -h localhost -p 5432

# Connect to PostgreSQL
psql -h localhost -U postgres -p 5432
```

### 2. Database Creation
Create the required databases:
```sql
-- Connect to PostgreSQL as postgres user
psql -h localhost -U postgres -p 5432

-- Create development database
CREATE DATABASE msmq_manager;

-- Create test database
CREATE DATABASE msmq_manager_test;

-- Verify databases
\l

-- Exit
\q
```

## Flyway Migration

### Migration Files Location
```
src/main/resources/db/migration/
├── V1__Create_initial_schema.sql
└── V2__Add_additional_indexes_and_constraints.sql
```

### Migration Execution
Flyway will automatically execute migrations when the application starts. The migration process:

1. **Checks current schema version** in `flyway_schema_history` table
2. **Executes pending migrations** in version order
3. **Updates schema version** after successful migration
4. **Validates** the final schema

### Manual Migration (Optional)
If you need to run migrations manually:
```bash
# Using Flyway CLI (if installed)
flyway -url=jdbc:postgresql://localhost:5432/msmq_manager \
       -user=postgres \
       -password=Approved12@ \
       migrate

# Or using Maven
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/msmq_manager \
                   -Dflyway.user=postgres \
                   -Dflyway.password=Approved12@
```

## Database Schema

### Core Tables

#### 1. `msmq_queue_config`
Stores MSMQ queue configuration and settings.
- **Primary Key**: `id` (auto-incrementing)
- **Unique Constraint**: `queue_name`
- **Key Fields**: queue_name, queue_path, queue_type, is_transactional, is_durable

#### 2. `msmq_message_audit`
Comprehensive audit trail for all MSMQ operations.
- **Primary Key**: `id`
- **Key Fields**: request_id, queue_name, operation_type, operation_status, response_code
- **Indexes**: Optimized for querying by queue, time, and status

#### 3. `msmq_connection_session`
Tracks MSMQ connection sessions and their status.
- **Primary Key**: `id`
- **Unique Constraint**: `session_id`
- **Key Fields**: session_id, connection_host, connection_port, connection_status

#### 4. `msmq_performance_metrics`
Stores performance metrics for monitoring and analysis.
- **Primary Key**: `id`
- **Key Fields**: metric_name, metric_value, metric_unit, queue_name, timestamp

#### 5. `msmq_error_statistics`
Aggregates error statistics for monitoring and alerting.
- **Primary Key**: `id`
- **Key Fields**: error_code, error_message, error_count, severity

#### 6. `msmq_system_health`
Monitors system health status for various components.
- **Primary Key**: `id`
- **Key Fields**: health_check_name, health_status, component_name

### Views

#### 1. `v_queue_statistics`
Provides summary statistics for each queue including operation counts and performance metrics.

#### 2. `v_error_summary`
Provides aggregated error statistics across all queues.

#### 3. `v_system_health_overview`
Provides recent system health status for all components.

## Configuration

### Application Properties
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/msmq_manager
    driver-class-name: org.postgresql.Driver
    username: postgres
    password: Approved12@
  
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway manages schema, Hibernate only validates
  
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
    locations: classpath:db/migration
    table: flyway_schema_history
    baseline-version: 0
```

### Flyway Configuration Options
- **`enabled`**: Enable/disable Flyway migrations
- **`baseline-on-migrate`**: Automatically baseline existing databases
- **`validate-on-migrate`**: Validate migrations before execution
- **`locations`**: Path to migration scripts
- **`table`**: Name of the schema history table
- **`baseline-version`**: Starting version for baseline

## Data Validation

### Check Constraints
The schema includes comprehensive check constraints:
- Queue name length (1-124 characters)
- Queue path length (1-500 characters)
- Message size limits (0-100MB)
- Timeout values (0-5 minutes)
- Valid operation types and statuses
- Valid connection statuses and port ranges

### Indexes
Performance-optimized indexes for:
- Primary key lookups
- Queue name queries
- Time-based queries
- Status-based filtering
- Composite queries (queue + time)

## Monitoring and Maintenance

### Schema Version Tracking
```sql
-- Check current schema version
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;

-- View all applied migrations
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### Database Health Checks
```sql
-- Check table sizes
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats 
WHERE schemaname = 'public' 
ORDER BY tablename, attname;

-- Check index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes 
ORDER BY idx_scan DESC;
```

### Backup and Recovery
```bash
# Create database backup
pg_dump -h localhost -U postgres -d msmq_manager > msmq_manager_backup.sql

# Restore database
psql -h localhost -U postgres -d msmq_manager < msmq_manager_backup.sql
```

## Troubleshooting

### Common Issues

#### 1. Connection Refused
```bash
# Check if PostgreSQL is running
sudo systemctl status postgresql

# Start PostgreSQL if stopped
sudo systemctl start postgresql
```

#### 2. Authentication Failed
```bash
# Check pg_hba.conf configuration
# Ensure local connections are allowed for postgres user
```

#### 3. Migration Failures
```bash
# Check Flyway logs in application startup
# Verify migration scripts syntax
# Check database permissions
```

#### 4. Schema Validation Errors
```sql
-- Check if tables exist
\dt

-- Check table structure
\d msmq_queue_config

-- Verify constraints
SELECT conname, contype, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conrelid = 'msmq_queue_config'::regclass;
```

## Security Considerations

### Database User Permissions
```sql
-- Create dedicated application user (recommended)
CREATE USER msmq_app_user WITH PASSWORD 'secure_password';

-- Grant necessary permissions
GRANT CONNECT ON DATABASE msmq_manager TO msmq_app_user;
GRANT USAGE ON SCHEMA public TO msmq_app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO msmq_app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO msmq_app_user;
```

### Connection Security
- Use SSL connections in production
- Implement connection pooling
- Regular password rotation
- Network-level access controls

## Performance Optimization

### Connection Pooling
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Query Optimization
- Use prepared statements
- Implement pagination for large result sets
- Regular index maintenance
- Monitor slow query logs

## Next Steps

1. **Start the application** - Flyway will automatically create the schema
2. **Verify tables** - Check that all tables and indexes are created
3. **Test operations** - Verify CRUD operations work correctly
4. **Monitor performance** - Use the provided views for monitoring
5. **Set up alerts** - Configure monitoring for database health

## Support

For database-related issues:
1. Check application logs for Flyway migration details
2. Verify PostgreSQL connection and permissions
3. Review migration script syntax
4. Check database constraints and indexes
5. Monitor performance metrics
