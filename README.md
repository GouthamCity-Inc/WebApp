
# WebApp


### CSYE 6225 Fall 2023 Project  
---

## Prerequisites

The following are required in order to provide a successful runtime for the application:  

1. Java Development Kit (JDK) 17 or higher

2. [Gradle ](https://gradle.org/)

  

## Configs
  

1. Clone the repo into your local file system

2. Configure MariaDB as the Database for the application:

  
- For now, the DB credentials are hard-code and reside in the `application.properties` file located in the `src/main/resources` directory of the app. However, they can be overridden at runtime through command line arguements

- These are the DB specific properties:

```properties

spring.datasource.url=jdbc:mariadb://localhost:3306/yourdb
spring.datasource.username=username
spring.datasource.password=password
```

Specify the complete file location to load users into accounts table

```properties
application.config.csv-file=${USERS_CSV:users.csv}
```
  
  

## Build

  

To build the Spring Boot application, use the following Gradle command:

  

```bash
.gradlew clean build -xTest
```

  

## Run


###  Docker Compose to bring up the DB

  

A Docker Compose file is available in the root of the application path to boot up a MariaDB instance via Docker runtime. To use Docker Compose, follow these steps:

1. Ensure Docker host is up and running in your system

2. Execute the following command from the root of the application:

```bash

docker-compose up

```

This will spin up a MariaDB instance on your local machine




### Gradle run

  

To run the application using Gradle, execute the following command:

  
```bash
./gradlew  bootRun
```

**Note**: The DB must be up before starting the application, else it will crash due to Hibernate exceptions. The DB must always be in a healthy state prior to application start.

One the app starts successfully,  it can be accessed at the following address: `http://localhost:8080`. 

 
  
  

## Local integration test

  

[Karate](https://github.com/intuit/karate) tests can be executed via the following command, which tests the application's health endpoint :



```bash

gradle  test

```

 

## License

  

This repository uses MIT License. See the [LICENSE](LICENSE) file for details.

  
