<h2>BioModels Insertion</h2>

The BioModels step is run following the <a href="https://github.com/reactome/analysis-core">analysis-core</a> and before UpdateGKCurrent (link needed) in Reactome's quarterly data release. It consists of two parts: (1) running the <a href="https://github.com/reactome/biomodels-mapper">biomodels-mapper</a> to generate `models2pathways.tsv` and than (2) running the code found in this repository to perform database updates. This is a rewrite of <a href="https://github.com/reactome/Release/blob/master/scripts/release/biomodels/add_links_to_biomodels_resource.pl">add_links_to_biomodels_resource.pl</a>

The database updates performed are based on the contents of `models2pathways.tsv`. This is basically a mapping file of Reactome's pathways against the mathematical systems that exist in EBI's <a href="https://www.ebi.ac.uk/biomodels/">BioModels</a> resource.

```
## models2pathways.tsv example
BIOMD0000000498 R-HSA-917977    8.191346134722721E-7    https://reactome.org/PathwayBrowser/#/R-HSA-917977      Transferrin endocytosis and recycling   IEA     Homo sapiens
BIOMD0000000287 R-HSA-69563     6.066213418698396E-6    https://reactome.org/PathwayBrowser/#/R-HSA-69563       p53-Dependent G1 DNA Damage Response    IEA     Homo sapiens
BIOMD0000000287 R-HSA-2559586   6.066213418698396E-6    https://reactome.org/PathwayBrowser/#/R-HSA-2559586     DNA Damage/Telomere Stress Induced Senescence   IEA     Homo sapiens
```

For the purposes of this module, the first two columns are the most important. The first column contains BioModels identifiers, and the second column contains Reactome Pathway identifiers that have have been connected to them by biomodels-mapper. For each line in `models2pathways.tsv`, the corresponding Pathway instance in Reactome's relationship database will receive a CrossReference that points to the biomodel identifier. If the biomodel identifier does not already have a database instance, it will be created before being slotted into the Pathway instance as an instance of type `DatabaseIdentifier`. All biomodels identifier instances are connected to the sole 'BioModels Database' `ReferenceDatabase` object. 

<h3> Preparing and running Biomodels Insertion </h3>

First, the jar file needs to be built with its dependencies:

`mvn clean compile assembly:single`

After the jar has built successfully, there are two other files that need to be provided as arguments: the configuration file and the models2pathways.tsv.

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
Once this has been completed and saved somewhere on the server, you should be able to run the biomodels insertion step:

`java -jar target/biomodels-1.0-SNAPSHOT-jar-with-dependencies.jar path/to/configuration/file path/to/models2pathways.tsv`

<h3> Validating BioModels Insertion </h3>

The insertion step generally takes a few minutes. Once it has completed, the simplest way to verify that it ran correctly is through Reactome's <a href="https://reactome.org/download-data/reactome-curator-tool">Curator Tool</a>. Once you have loaded up the database, first confirm that a 'BioModels Database' instance exists in the `ReferenceDatabase` class. This should have been created during the insertion step, if it didn't exist already. By right-clicking on the BioModels Database instance, you are able to 'Display Referrers'. This should bring up a list of `DatabaseIdentifier` objects that look like 'BioModels Database:BIOMD########'. Confirm that there are no duplicates in this list and compare the count in this release to the previous release. The numbers should be similar, if not a bit higher for the most recent one. That is everything that needs to be done for validation! Next up is the UpdateGKCurrent (link needed) step. 
