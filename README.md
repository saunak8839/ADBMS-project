# Telecom Data Pipeline

A project to handle call data generation, tracking and analysis.

## Setup
### Infrastructure
Check `infra/` for Docker and Database configurations.

### Services
- **Producer**: Generates CDRs and publishes them to Kafka
- **Consumer**: Validates, persists, and detects fraud

### Frontend Dashboard
- Located in `dashboard-ui/`
