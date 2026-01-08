# 4. Retrospective

This section reflects on the development process, lessons learned, and areas for future improvement.

## What Went Well

### Technical Achievements

| Achievement | Impact |
|-------------|--------|
| **87% Test Coverage** | Exceeded 70% requirement by 17%, ensuring code reliability |
| **Multi-provider Architecture** | Successfully implemented fallback chain with 5 providers |
| **Clean Microservices Separation** | Each service has clear responsibility and independent deployment |
| **Comprehensive API Documentation** | 12 documentation files covering all endpoints with examples |
| **Docker Containerization** | One-command deployment with health checks and resource limits |

### Process Wins

1. **Incremental Development**: Building features iteratively allowed early detection of integration issues
2. **Test-Driven Approach**: Writing tests alongside code improved design quality
3. **Documentation-First API Design**: OpenAPI specs before implementation reduced rework
4. **Early Containerization**: Docker setup from day one prevented "works on my machine" issues

### Architecture Decisions That Paid Off

| Decision | Benefit |
|----------|---------|
| API Gateway pattern | Centralized authentication, simplified client integration |
| Database-per-service | Independent schema evolution, clear ownership |
| Median rate calculation | Eliminated outlier rates, improved accuracy |
| Liquibase migrations | Version-controlled schema, reproducible environments |

## What Could Be Improved

### Technical Debt

| Area | Current State | Ideal State | Priority |
|------|---------------|-------------|----------|
| **Rate Caching** | In-memory per instance | Redis distributed cache | High |
| **Service Discovery** | Docker DNS | Consul/Eureka for production | Medium |
| **Configuration** | Environment variables | Spring Cloud Config | Medium |
| **Logging** | Local files | ELK stack centralized | Medium |
| **Monitoring** | Basic actuator | Prometheus + Grafana | Low |

### Code Quality Issues

1. **Some large service classes**: `CurrencyService` could be split into smaller focused classes
2. **Duplicate DTOs**: Similar request/response objects across services could use shared library
3. **Hardcoded timeouts**: External API timeouts should be configurable
4. **Limited retry configuration**: Retry policies are basic, could use Resilience4j fully

### Missing Features

| Feature | Reason Not Implemented | Future Consideration |
|---------|----------------------|---------------------|
| Rate limiting | Time constraints | Essential for production |
| WebSocket updates | REST sufficient for diploma | Nice-to-have |
| Audit logging | Not in requirements | Important for compliance |
| API versioning | Single version sufficient | Required for evolution |

## Challenges Faced

### Challenge 1: External API Rate Limits

**Problem**: Free-tier APIs limited to 100-1000 requests/month

**Solution**: 
- Implemented aggressive caching (1-hour TTL)
- Created mock services for development/testing
- Scheduled updates instead of on-demand fetching

**Lesson**: Always have fallback strategy for external dependencies

### Challenge 2: JWT Validation Across Services

**Problem**: Each service needed to validate tokens independently

**Solution**:
- Centralized validation in API Gateway
- Gateway propagates user context via headers
- Services trust internal network headers

**Lesson**: API Gateway is essential for cross-cutting concerns

### Challenge 3: Test Database Compatibility

**Problem**: H2 in-memory database behavior differs from PostgreSQL

**Solution**:
- Used H2's PostgreSQL compatibility mode
- Focused tests on business logic, not SQL specifics
- Integration tests with real PostgreSQL for critical paths

**Lesson**: Test database should match production as closely as possible

### Challenge 4: Docker Compose Startup Order

**Problem**: Services starting before databases were ready

**Solution**:
- Implemented health checks for all containers
- Used `depends_on` with `condition: service_healthy`
- Added appropriate `start_period` for applications

**Lesson**: Health checks are essential for container orchestration

## Lessons Learned

### Technical Lessons

1. **Start with contracts**: Define API contracts before implementation saves rework
2. **Test external integrations early**: WireMock setup on day one prevented surprises
3. **Automate everything**: CI/CD from the start catches issues early
4. **Document as you go**: Writing docs after the fact is harder and less accurate
5. **Security from the start**: Adding security later is much harder than building it in

### Process Lessons

1. **Time estimation**: Microservices take longer than monolith - budget accordingly
2. **Scope management**: "Nice to have" features can wait for v2
3. **Iterative delivery**: Working software beats perfect plans
4. **Ask for help**: Supervisor feedback early prevented wrong directions

### What I Would Do Differently

| Then | Now |
|------|-----|
| Started with monolith, split later | Would start with microservices from day one |
| Manual testing initially | Would set up CI/CD on day one |
| Documentation at the end | Would write docs alongside code |
| Complex external API clients | Would use simpler HTTP clients with retry |

## Future Improvements

### Short-term (Next Release)

- [ ] Add Redis for distributed caching
- [ ] Implement rate limiting per user
- [ ] Add request/response logging
- [ ] Improve error messages for validation

### Medium-term (Future Development)

- [ ] Kubernetes deployment manifests
- [ ] Prometheus metrics + Grafana dashboards
- [ ] Event-driven updates with Kafka
- [ ] API versioning (v2 endpoints)

### Long-term (Production Readiness)

- [ ] Multi-region deployment
- [ ] Disaster recovery procedures
- [ ] Performance load testing
- [ ] Security audit and penetration testing

## Metrics Summary

| Metric | Target | Achieved | Status |
|--------|--------|----------|--------|
| Test Coverage | ≥70% | 87% | Exceeded |
| Services | ≥3 | 6 | Exceeded |
| API Documentation | Complete | 12 files | Complete |
| Docker Containers | Required | 9 | Complete |
| External Providers | ≥1 | 5 | Exceeded |

## Acknowledgments

- **Supervisor**: Jan Bartnitsky (UAB Gurtam) - guidance on architecture decisions
- **EPAM Mentors**: Support during Digital Engineering School
- **EHU Faculty**: Academic guidance and requirements clarification

## Conclusion

The CERPS project successfully demonstrates enterprise-grade microservices architecture with comprehensive testing, security, and documentation. While there is technical debt to address for production deployment, the core functionality meets all diploma requirements and provides a solid foundation for future enhancement.

The key takeaway: **building for reliability from the start** (multi-provider fallback, high test coverage, containerization) paid off in reduced debugging time and confidence in the final product.
