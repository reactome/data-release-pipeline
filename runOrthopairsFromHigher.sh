cd orthopairs
bash updateOrthopairsConfig.sh -r 69
mvn clean compile assembly:single
java -jar target/orthopairs-0.0.1-SNAPSHOT-jar-with-dependencies.jar
