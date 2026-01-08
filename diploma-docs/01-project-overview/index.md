# 1. Project Overview

This section covers the business context, goals, and requirements for the CERPS project.

## Contents

- [Problem Statement & Goals](problem-and-goals.md)
- [Stakeholders & Users](stakeholders.md)
- [Scope](scope.md)
- [Features](features.md)

## Executive Summary

The Currency Exchange Rate Provider Service (CERPS) addresses critical challenges faced by organizations using currency exchange APIs: reliability issues from single-provider dependency, data accuracy concerns, and vendor lock-in problems. 

The solution aggregates exchange rates from multiple providers (Fixer.io, ExchangeRatesAPI, CurrencyAPI, and 2 mock fallback services), calculates median rates for accuracy, and provides automatic failover for high availability. Target users include e-commerce platforms, fintech developers, and financial analysts who need reliable currency conversion services.

Key outcomes include 99.9% target uptime through multi-provider redundancy, accurate rates via median calculation, role-based access control (USER, PREMIUM_USER, ADMIN), and comprehensive API documentation.

## Key Highlights

| Aspect | Description |
|--------|-------------|
| **Problem** | Single-provider currency APIs create reliability risks and data accuracy issues |
| **Solution** | Multi-provider aggregation with median calculation and automatic fallback |
| **Target Users** | E-commerce CTOs, Fintech Developers, Financial Analysts |
| **Key Features** | Currency conversion, trend analysis, encrypted API key management, JWT authentication |
| **Tech Stack** | Java 21, Spring Boot 3.5.6, PostgreSQL 15, Docker, JWT, AES-256-GCM |
