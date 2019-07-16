./gradlew clean shadowJar
docker build --tag=lightning-store .

heroku container:push --app lightning-store web
heroku container:release --app lightning-store web
