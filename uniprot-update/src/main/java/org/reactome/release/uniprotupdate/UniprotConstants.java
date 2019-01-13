package org.reactome.release.uniprotupdate;

import java.util.Arrays;
import java.util.List;

public class UniprotConstants {
    public static final String UNIPROT = "UniProt";
    public static final String ENSEMBL_HOMO_SAPIENS_GENE = "ENSEMBL_Homo_sapiens_GENE";
    public static final String HOMO_SAPIENS = "Homo sapiens";

    // List of species names was taken from uniprot_xml2sql_isoform.pl:84 // TODO: move these values to a resource file.
    public static final List<String> SPECIES_TO_UPDATE = Arrays.asList(
        HOMO_SAPIENS, "Mus musculus", "Rattus norvegicus",
        "Bos taurus", "Gallus gallus", "Drosophila melanogaster",
        "Caenorhabditis elegans", "Saccharomyces cerevisiae", "Schizosaccharomyces pombe",
        "Human immunodeficiency virus type 1", "Human immunodeficiency virus type 2", "Influenza A virus"
    );

}
