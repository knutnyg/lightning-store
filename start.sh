git pull
cd src/main/frontend || exit
npm run build
cd ../../../
./gradlew clean jar
java -jar build/libs/app-1.1.jar