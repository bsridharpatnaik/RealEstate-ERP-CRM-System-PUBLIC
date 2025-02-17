# RealEstate-ERP-CRM-System

A comprehensive Real Estate ERP (Enterprise Resource Planning) and CRM (Customer Relationship Management) system built using a microservices architecture. It provides a suite of tools for managing customer relationships, inventory, and project planning in the real estate industry.

## Key Features

- Customer Relationship Management (CRM)
  - Lead Management
  - Deal Structure
  - Customer Activities
- Inventory Management
  - Stock Management
  - Inward/Outward Inventory
  - Lost or Damaged Inventory Tracking
- Bill of Quantities (BOQ) Module
- Dashboard and Reporting
- Multi-tenancy Support

## System Architecture

The system is built on a microservices architecture, consisting of the following main components:

### CRM Service

Handles customer relationship management tasks, including:
- Lead Management
- Deal Structure
- Customer Activities
- Notes and Activities tracking

Key files:
- `LeadRepo.java`: Repository for lead-related database operations
- `NoteRepo.java`: Repository for managing customer notes
- `DealStructureRepo.java`: Repository for deal structure management

### Inventory Service

Manages inventory-related operations, including:
- Stock Management
- Inward/Outward Inventory
- Machinery and Equipment tracking
- Lost or Damaged Inventory handling

Key files:
- `MachineryRepo.java`: Repository for machinery-related database operations
- `InventoryNotificationRepo.java`: Repository for inventory notifications
- `StockHistoryRepo.java`: Repository for tracking stock history

### Common Service

Provides shared functionalities across services:
- Security Configuration
- User Management
- Multi-tenancy Support
- Notification Management

Key files:
- `NotificationHistoryRepo.java`: Repository for managing notification history
- `TenantRepo.java`: Repository for multi-tenancy support

### Gateway Application

Acts as the entry point for the microservices, handling routing and load balancing.

## Technologies Used

- Java
- Spring Boot
- Spring Data JPA
- Maven
- MySQL (assumed based on JPA repositories)

## Getting Started

1. Clone the repository
2. Set up the database (MySQL recommended)
3. Configure application properties for each service
4. Build the projects using Maven
5. Run each service individually

## Contact

bsridharpatnaik@gmail.com

Note: Please add appropriate license and contact information before making the repository public.
