

##Runtime Environment:
Lightning store assuses you have the following runtime environment variabels:

- host i.e "localhost"
- port i.e "8080"
- tls_cert base64 encoded
- macaroon base64 encoded

##Build & Deploy
1. `./gradlew clean shadowJar`
2. `docker build --tag=lightning-store .`
3. `docker run lightning-store` (optional)

4. `heroku container:push web`
5. `heroku container:release web` 

