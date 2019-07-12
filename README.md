

##Runtime Environment:
Lightning store assumes you have the following runtime environment variables:

- host i.e "localhost"
- port i.e "8080"
- tls_cert base64 encoded
- macaroon base64 encoded
- DATABASE_URL i.e "postgres://user@localhost:5432/"

##Postgres
The application expects you to have postgres running locally.

##Build & Deploy
1. `./gradlew clean shadowJar`
2. `docker build --tag=lightning-store .`
3. `docker run lightning-store` (optional)

4. `heroku container:push web`
5. `heroku container:release web` 

