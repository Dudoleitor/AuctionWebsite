# AuctionWebsite
## Overview

This repository contains the source code for a web application developed as part of the "Information Technologies for the Web" course project at Politecnico di Milano. The application is designed to be deployed in a Kubernetes (k8s) cluster, utilizing Docker for containerization. The project demonstrates key technologies and patterns for modern web applications, including database interactions, session management, caching, and security best practices.

The goal of this project is to demonstrate the application of modern web technologies in a scalable, containerized, and secure environment.

## Features

- **Web Application**: The core functionality allows users to interact with a dynamic marketplace, managing auctions and bids.
- **Kubernetes Deployment**: The application is deployed to a Kubernetes cluster for high availability and scalability.
- **Docker**: The application is containerized using Docker, making it easy to deploy in any environment.
- **Database Integration**: Interactions with a production-level database are handled securely and efficiently, with considerations for session management and connection pooling.
- **Caching**: Static assets (e.g., CSS, JS) are cached to improve performance and reduce network traffic.
- **Security**: Features include hashing of passwords, protection against brute-force attacks, and secure database credentials management.
- **Session Management**: Distributed sessions across multiple Kubernetes nodes, utilizing Tomcat's clustering features.

## Technologies Used

- **Javascript**, **HTML&CSS** and **Java**
- **Tomcat** (Web server and servlet container)
- **Docker** (Containerization)
- **Kubernetes** (Container orchestration)
- **MySQL** (Database)
- **Apache Maven** (Build automation)
- **Apache Tomcat ExpiresFilter** (Caching static assets)

The project includes two versions: a pure HTML version and a JavaScript Rich Internet Application (RIA) version. The HTML version is simpler, with content generated dynamically by the server, while the JavaScript version enhances user interactivity and performance by caching resources and reducing network requests. In this version, JavaScript handles the client-side logic, while the backend (Java) interacts with the database and manages the application state.

This project provides an invaluable learning opportunity, bridging the gap between different layers of web development. From frontend development with JavaScript (creating dynamic user interfaces) to backend development with Java (handling business logic and data processing), to SQL and database design (optimizing storage and queries), and finally to deployment in a Kubernetes environment (ensuring scalability and high availability). This full-stack approach highlights how each component interacts and the importance of understanding the entire web development workflow.

### Session Management

To manage user sessions across multiple pods, the project uses Tomcat's clustering features. Each Tomcat instance communicates with other instances within the Kubernetes cluster to replicate session data.

The session state can also be stored in a persistent volume if needed, or managed through a shared database.

### Security Considerations

- **Password Hashing**: Passwords are stored securely using the SHA1 hashing algorithm. This is a basic measure of security for demonstration purposes. In a real-world application, stronger hashing algorithms such as SHA256 or bcrypt should be used.
- **Brute-force Protection**: The application logs failed login attempts and is ready to be integrated with Crowdsec for further protection against malicious access attempts.
- **Database Security**: Database credentials are managed using Kubernetes Secrets to ensure they are not exposed in the codebase.

### Additional Features

- **Tomcat ExpiresFilter**: Static assets such as CSS and JS files are cached on the client-side to improve application performance.
- **Persistent Storage**: Images and other files related to auctions are stored in a persistent volume to ensure that data is not lost during pod restarts.
- **Load Balancing**: Kubernetes Service and Ingress resources are configured to provide load balancing and high availability for the application.
