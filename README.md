# TODO! Adstart Media Senior Java Developer Technical Assignment

## Read Me First / Intro (TODO)

## Getting Started (TODO)

### Running application with Docker (one-click start for 10s)

```bash
  ./docker/clean-postgres-start.sh
  docker compose up
```

or to start the container in "detached" mode:

```bash
  ./docker/clean-postgres-start.sh
  docker compose up -d
```

### Running application from IDE

1. Clean DB start (for local development):

```bash
  ./docker/clean-postgres-start.sh
```

2. Building of Docker image

```bash
  ./gradlew jibDockerBuild
```

3. Set environment in IDE:

```
DATASOURCE_PASSWORD=App!Pswd;DATASOURCE_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME};DATASOURCE_USERNAME=app;DB_HOST=localhost;DB_NAME=domain_watchdog;DB_PORT=5433
```

4. Run the application from IDE


---------
### Using application guide (TODO or remove)


### Project Structure
domain‑watchdog/
├── .env           ← сюда
├── cloudformation/
│   ├── domain-watchdog.yml
│   └── README.md
├── docker/
│   └── docker‑compose.yml
        todo
│  
└── gradle/
└── src/
└── 



The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)

### Additional Links

These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
