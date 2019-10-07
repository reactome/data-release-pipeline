# BioModels Data Insertion Release Step

The BioModels step is run following the <a href="https://github.com/reactome/analysis-core">analysis-core</a> and before UpdateGKCurrent (link needed) in Reactome's quarterly data release. It consists of two parts: (1) running the <a href="https://github.com/reactome/biomodels-mapper">biomodels-mapper</a> to generate `models2pathways.tsv` and than (2) running the code found in this repository to perform database updates. This is a rewrite of <a href="https://github.com/reactome/Release/blob/master/scripts/release/biomodels/add_links_to_biomodels_resource.pl">add_links_to_biomodels_resource.pl</a>

This tool will insert the cross-references for the BioModels Reference Database into the appropriate pathway instances of the release database.

It will:

 - Read the models2pathways.tsv file describing the relationships between Reactome pathways and BioModels identifiers.
 - Create a BioModels Reference Database instance (if one does not already exist)
 - Create the Database Identifier instances representing the BioModels identifiers and insert them into Pathway instances under the 'crossReference' attribute
 
 ```
## models2pathways.tsv example
BIOMD0000000498 R-HSA-917977    8.191346134722721E-7    https://reactome.org/PathwayBrowser/#/R-HSA-917977      Transferrin endocytosis and recycling   IEA     Homo sapiens
BIOMD0000000287 R-HSA-69563     6.066213418698396E-6    https://reactome.org/PathwayBrowser/#/R-HSA-69563       p53-Dependent G1 DNA Damage Response    IEA     Homo sapiens
BIOMD0000000287 R-HSA-2559586   6.066213418698396E-6    https://reactome.org/PathwayBrowser/#/R-HSA-2559586     DNA Damage/Telomere Stress Induced Senescence   IEA     Homo sapiens
```

For the purposes of this module, the first two columns are the most important. The first column contains BioModels identifiers, and the second column contains Reactome Pathway identifiers that have have been connected to them by biomodels-mapper. For each line in `models2pathways.tsv`, the corresponding Pathway instance in Reactome's relationship database will receive a CrossReference that points to the biomodel identifier. If the biomodel identifier does not already have a database instance, it will be created before being slotted into the Pathway instance as an instance of type `DatabaseIdentifier`. All biomodels identifier instances are connected to the sole 'BioModels Database' `ReferenceDatabase` object. 
 
## Logging
 
This application will log to a file under `./logs/biomodels-{timestamp}.log` and `./logs/biomodels-{timestamp}.err` for warnings and errors. This file will have the same content as the console (stdout/stderr) except without the log4j prefix. 
 
## Configuration

The configuration file is set up like a typical properties file, containing database adaptor information and the database ID of your person instance:

```
### Sample configuration file with properties
release.database.host=mySQLHost
release.database.user=mySQLUsername
release.database.password=mySQLPassword
release.database.port=3306
release_current.name=release_current
personId=123456
```
Once this has been completed and saved somewhere on the server, you should be able to run the biomodels insertion step.
  
## Compiling & Running

This is a Java application which requries a Java 8+ environment. You will need maven and a full JDK to compile.

To compile the application, run this command:

```
$ mvn clean compile package
```

If this is successful, you should see a JAR file in the `target` directory, with a name like `biomodels-VERSION_NUMBER-jar-with-dependencies.jar`. This is the file you will run.

For example, for version "1.0-SNAPSHOT", execute this command to run the program:
```
$ java -jar target/biomodels-1.0-SNAPSHOT-jar-with-dependencies.jar ./config.properties path/to/models2pathways.tsv
```

If no properties file is specified as the first argument, the program will look in src/main/resources/ for config.properties. Similarly for models2pathways.tsv, unless otherwise specified it will assume it exists in src/main/resources.

## Validating BioModels Insertion </h3>

The insertion step generally takes a few minutes. Once it has completed, the simplest way to verify that it ran correctly is through Reactome's <a href="https://reactome.org/download-data/reactome-curator-tool">Curator Tool</a>. Once you have loaded up the database, first confirm that a 'BioModels Database' instance exists in the `ReferenceDatabase` class. This should have been created during the insertion step, if it didn't exist already. By right-clicking on the BioModels Database instance, you are able to 'Display Referrers'. This should bring up a list of `DatabaseIdentifier` objects that look like 'BioModels Database:BIOMD########'. Confirm that there are no duplicates in this list and compare the count in this release to the previous release. The numbers should be similar, if not a bit higher for the most recent one. That is everything that needs to be done for validation! Next up is the UpdateGKCurrent (link needed) step. 
