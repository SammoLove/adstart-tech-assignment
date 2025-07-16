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
### Using application guide
There three endpoints:
1. Add, save and process a bunch of domains
```
POST {{host}}/api/v1/domains
Body example:
[
        {"name": "adstartmedia.com"},
        {"name": "amazon.com"},
        {"name": "test-ev-rsa.ssl.com"},
        {"name": "expired-rsa-dv.ssl.com"},
        {"name": "mailbase.eu"},
        {"name": "mail.albijjar.com"}
]
Returns 201
```
2. Getting all domains with results we have
```
GET {{host}}/api/v1/domains
Returns f.i. JSON array like this:
[
    {
        "name": "adstartmedia.com",
        "expirationTime": "2026-03-08T23:59:59Z",
        "expirationStatus": "OK"
    },
    {
        "name": "amazon.com",
        "expirationTime": "2026-06-19T23:59:59Z",
        "expirationStatus": "OK"
    },
    {
        "name": "test-ev-rsa.ssl.com",
        "expirationTime": "2026-07-05T16:06:04Z",
        "expirationStatus": "OK"
    },
    {
        "name": "expired-rsa-dv.ssl.com",
        "expirationTime": "2016-08-02T20:48:30Z",
        "expirationStatus": "EXPIRED"
    },
    {
        "name": "mailbase.eu",
        "expirationTime": "2025-08-21T06:36:54Z",
        "expirationStatus": "NOTICE"
    },
    {
        "name": "mail.albijjar.com",
        "expirationTime": "2025-08-24T11:19:47Z",
        "expirationStatus": "NOTICE"
    }
]
```
3. Getting one
```
GET {{host}}/api/v1/domains/expired-rsa-dv.ssl.com
Returns f.i. 
{
    "name": "expired-rsa-dv.ssl.com",
    "expirationTime": "2016-08-02T20:48:30Z",
    "expirationStatus": "EXPIRED"
}
```

### Project Structure
```
domain‑watchdog/
├── cloudformation/
│   ├── domain-watchdog.yml
│   └── README.md
│
├── docker/
│   ├── docker‑compose.yml
│   ├── docker‑compose.prod.yml
│   ├── docker‑compose.db.yml
│   ├── .env
│   └── clean-postgres-start.sh
│
├── src/
│   ├── main/
│   └── test/
│
├── gradle/
│
├── pom.xml
├── build.gradle
├── .env
└── README.md
```
