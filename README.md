
# Microservices Architecture Fundamentals

## Overview

This project demonstrates the fundamental difference between **Monolithic Architecture** and **Microservices Architecture** using Spring Boot applications.

The repository contains:

1. A simple **Monolithic Application**
2. Multiple independent **Microservices**

The purpose of this project is to understand how an application can evolve from a single monolithic application into independently developed and deployed microservices.

---

# Project Architecture

```text
microservices-architecture-fundamentals
│
├── microservices
│   ├── user-service
│   ├── order-service
│   ├── product-service
│   ├── payment-service
│   └── notification-service
│
└── monolith
    └── monolithic-app

1. Monolithic Architecture

The monolithic-app represents a traditional monolithic application.

In a monolithic architecture, multiple business modules are developed and deployed as part of a single application.

monolithic-app
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── monolithic_app
│   │   │       ├── order
│   │   │       │   └── OrderController.java
│   │   │       │
│   │   │       ├── product
│   │   │       │   └── ProductController.java
│   │   │       │
│   │   │       ├── user
│   │   │       │
│   │   │       └── MonolithicAppApplication.java
│   │   │
│   │   └── resources
│   │       └── application.properties
│   │
│   └── test
│
└── pom.xml

Available Modules

The monolithic application contains business modules such as:

* User Module
* Order Module
* Product Module

These modules exist inside a single Spring Boot application.

Sample Endpoints

Order Module
GET/orders
example response:
Order module is working

Product module
GET/product
example response:
Product is working
Running the Monolithic Application

Navigate to the monolithic application directory:
Running the Monolithic Application

Navigate to the monolithic application directory:
cd monolith/monolithic-app
Run the application using Maven:
mvnw.cmd spring-boot:run
The application can then be accessed through the configured Spring Boot port.
2. Microservices Architecture

The microservices directory contains multiple independent services.

Each service represents a separate business capability and can be developed, deployed, and scaled independently.

Microservices

The project contains the following services:
microservices
│
├── user-service
│
├── order-service
│
├── product-service
│
├── payment-service
│
└── notification-service

⸻

User Service

The User Service is responsible for user-related operations.

Example responsibilities include:

* User management
* User information handling
* User-related business logic

⸻

Order Service

The Order Service is responsible for order-related operations.

Example responsibilities include:

* Creating orders
* Managing order information
* Processing order-related business logic

⸻

Product Service

The Product Service is responsible for product-related operations.

Example responsibilities include:

* Managing products
* Providing product information
* Product-related business logic

⸻

Payment Service

The Payment Service is responsible for payment-related functionality.

Example responsibilities include:

* Processing payments
* Handling payment information
* Payment-related operations

⸻

Notification Service

The Notification Service is responsible for sending notifications related to application events.

Example responsibilities include:

* Order notifications
* Payment notifications
* User-related notifications

⸻

Architecture Comparison

Monolithic Architecture
                ┌─────────────────────────────┐
                │     Monolithic Application  │
                │                             │
                │  ┌───────────────────────┐  │
                │  │     User Module       │  │
                │  └───────────────────────┘  │
                │                             │
                │  ┌───────────────────────┐  │
                │  │     Order Module      │  │
                │  └───────────────────────┘  │
                │                             │
                │  ┌───────────────────────┐  │
                │  │    Product Module     │  │
                │  └───────────────────────┘  │
                │                             │
                └─────────────────────────────┘

                    ┌───────────────┐
                    │  User Service │
                    └───────────────┘
                           │
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼

┌───────────────┐  ┌───────────────┐  ┌─────────────────┐
│ Order Service │  │Product Service│  │ Payment Service │
└───────────────┘  └───────────────┘  └─────────────────┘
        │
        ▼
┌──────────────────────┐
│ Notification Service │
└──────────────────────┘




Technologies Used

* Java
* Spring Boot
* Maven
* Spring Web
* REST APIs
* Git
* GitHub
* SQL WORKBENCH
* Postman

Running Microservices 
run every service individually
let the service start on tomcat with its respective port number
open any browser and enter your http url and see the output
here each service has its own port numbers because every service is an individual application in microservices architecture.
Test your api in postman and database in workbench
