# Stakeholders & Users

## Target Audience

| Persona | Description | Key Needs |
|---------|-------------|-----------|
| E-commerce CTO | Technical leader at online retail company | Multi-currency support without downtime |
| Fintech Developer | Developer building financial applications | Reliable API without vendor lock-in |
| Financial Analyst | Professional analyzing currency markets | Historical trend data and accurate rates |
| End Customer | User making purchases in foreign currency | Accurate currency prices in transactions |

## User Personas

### Persona 1: Alex - E-commerce CTO

| Attribute | Details |
|-----------|---------|
| **Role** | Chief Technology Officer at mid-size e-commerce company |
| **Age** | 35-45 |
| **Tech Savviness** | High |
| **Goals** | Ensure 24/7 currency conversion availability for international customers |
| **Frustrations** | Previous provider downtime caused lost sales during Black Friday |
| **Scenario** | Needs to display prices in 10+ currencies, requires instant conversion at checkout |

### Persona 2: Maria - Fintech Developer

| Attribute | Details |
|-----------|---------|
| **Role** | Senior Backend Developer at fintech startup |
| **Age** | 28-35 |
| **Tech Savviness** | Very High |
| **Goals** | Integrate reliable currency API into payment processing system |
| **Frustrations** | Current provider changed API format, requiring emergency refactoring |
| **Scenario** | Building multi-currency wallet, needs consistent API with good documentation |

### Persona 3: David - Financial Analyst

| Attribute | Details |
|-----------|---------|
| **Role** | Currency analyst at investment firm |
| **Age** | 30-40 |
| **Tech Savviness** | Medium |
| **Goals** | Analyze currency trends to optimize exchange timing |
| **Frustrations** | No aggregated view from multiple sources, must check each provider manually |
| **Scenario** | Needs 7-day, 30-day, and yearly trend analysis for major currency pairs |

## Stakeholder Map

### High Influence / High Interest

- **E-commerce CTOs**: Primary decision makers for API adoption, budget authority
- **Fintech Developers**: Direct API consumers, provide technical feedback
- **Project Supervisor**: Ensures diploma requirements are met

### High Influence / Low Interest

- **Security Teams**: Concerned with authentication and data protection
- **DevOps Teams**: Responsible for deployment and monitoring

### Low Influence / High Interest

- **Financial Analysts**: Power users of trend analysis features
- **End Customers**: Benefit from accurate pricing but don't interact with API directly

### Low Influence / Low Interest

- **Marketing Teams**: May use conversion data for pricing strategies
- **Support Teams**: Handle user inquiries about rate discrepancies

## User Roles in System

| Role | Access Level | Features |
|------|--------------|----------|
| **USER** | Basic | View currencies, convert amounts |
| **PREMIUM_USER** | Extended | All USER features + trend analysis |
| **ADMIN** | Full | All features + add currencies, refresh rates, manage API keys |
