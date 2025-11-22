# Law Firm Management System

This project is a multi-tenant law firm management system designed specifically for Moroccan law firms. It utilizes Django and Django Rest Framework (DRF) to provide a robust backend solution for managing various aspects of a law firm, including authentication and tenant management.

## Features

- **Multi-Tenancy**: Supports multiple law firms (tenants) using the same database with separate schemas.
- **JWT Authentication**: Implements JSON Web Token (JWT) authentication for secure access.
  - **Cabinet Authentication**: Authenticates the law firm (cabinet) to return a token with tenant information.
  - **User Authentication**: Authenticates individual users (e.g., assistants or additional lawyers) within the cabinet.

## Project Structure

```
law-firm-management
├── apps
│   ├── authentication          # Handles user authentication
│   └── tenants                 # Manages tenant-specific data
├── config                      # Configuration files for the project
├── manage.py                   # Command-line utility for the project
└── requirements.txt            # Project dependencies
```
