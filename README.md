Lightning Store is a project showcasing different techniques for a website leveraging the power of the lightning network
for micro payments.

## Building

- Java 16
- Node 15.9.0

## Environment

Lightning store assumes to run on the same host as LND and bitcoind however both ips and ports can be configured. See
the `Config` class for posible configuration. To run against LND lightning store expects environement variables containing:

- PORT i.e 8080
- LS_HOST_URL i.e localhost
- LS_HOST_PORT i.e 10009
- LS_DATABASE_URL i.e postgresql://localhost:5432
- LS_READONLY_MACAROON base64 encoded
- LS_INVOICES_MACAROON base64 encoded
- LS_TLS_CERT base64 encoded

## Running locally

Lighting store can now be run through `LocalBootstrap.main()`. By default it exects a production like environment with
Postgres and LND available, but those can be replaced with embedded postgres and a stub implementation.

## Building and deploy

Backend:
```
./gradlew clean jar
java -jar build/libs/app-1.1.jar
```
Frontend:
Build static files and serve them from a server. I use serve.
```
npm install
npm run build && serve -p 8090 build
```

