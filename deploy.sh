./gradlew clean shadowJar
docker build --tag=lightning-store .

heroku container:push web
heroku container:release web
