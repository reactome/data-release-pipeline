# BioModels Data Insertion Release Step

This tool will insert the cross-references for the BioModels Reference Database into the appropriate pathway instances of the release database.

It will:

 - Read the models2pathways.tsv file describing the relationships between Reactome pathways and BioModels identifiers.
 - Create a BioModels Reference Database instance (if one does not already exist)
 - Create the Database Identifier instances representing the BioModels identifiers and insert them into Pathway instances under the 'crossReference' attribute
 
## Configuration

Details on configuration are to come

## Logging
 
This application will log to a file under `./logs/biomodels-{timestamp}.log` and `./logs/biomodels-{timestamp}.err` for warnings and errors. This file will have the same content as the console (stdout/stderr) except without the log4j prefix.
  
## Compiling & Running

This is a Java application which requries a Java 8+ environment. You will need maven and a full JDK to compile.

To compile the application, run this command:

```
$ mvn clean compile package
```

If this is successful, you should see a JAR file in the `target` directory, with a name like `biomodels-VERSION_NUMBER-jar-with-dependencies.jar`. This is the file you will run.

For example, for version "1.0-SNAPSHOT", execute this command to run the program:
```
$ java -jar target/biomodels-1.0-SNAPSHOT-jar-with-dependencies.jar ./config.properties
```

If no properties file is specified as the first argument, the program will look in src/main/resources/ for config.properties.
