package org.reactome.datafix;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.common.database.InstanceEditUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.*;

public class KEGGAddLinksMycobacteriumH37RvLinkFix {
    public static void main(String[] args) throws Exception {

        Properties props = new Properties();
        props.load(new FileInputStream("src/main/resources/config.properties"));

        String username = props.getProperty("release.database.user");
        String password = props.getProperty("release.database.password");
        String database = props.getProperty("release_current.name");
        String host = props.getProperty("release.database.host");
        int port = Integer.valueOf(props.getProperty("release.database.port"));
        long personId = Long.valueOf(props.getProperty("personId"));

        MySQLAdaptor dba = new MySQLAdaptor(host, database, username, password, port);

        Map<String, String> identifierToPrefixMap = new HashMap<>();

        GKInstance instanceEditInst = InstanceEditUtils.createInstanceEdit(dba, personId, "org.reactome.release");
        instanceEditInst.addAttributeValue(ReactomeJavaConstants.note, "In release 71, changed accessUrl in 'KEGG Gene M. tuberculosis H37Rv' refDB and the corresponding identifier in its referrals");
        dba.updateInstance(instanceEditInst);

        // mtv-mappings.txt is the result of grepping all uniprot files for 'mtv:'. Same was done for 'mtu:'.
        // Each of these lines (there are 178) maps to one of the identifiers we want to update.
        // Here I iterate through each line in each file, and put each identifier in a map that looks like <identifierWithoutPrefix, identifierWithPrefix>
        // Warning messages are informal, and just for fun.

        String strv;
        BufferedReader brv = new BufferedReader(new FileReader(new File("src/main/resources/mtv-mappings.txt")));
        while ((strv = brv.readLine()) != null) {
            String prefixWithIdentifier = strv.split("\t")[1];
            String identifier = prefixWithIdentifier.split(":")[1];
            if (identifierToPrefixMap.get(identifier) == null) {
                identifierToPrefixMap.put(identifier, prefixWithIdentifier);
            } else {
                System.out.println("MAYDAY!");
            }
        }

        String stru;
        BufferedReader bru = new BufferedReader(new FileReader(new File("src/main/resources/mtu-mappings.txt")));
        while ((stru = bru.readLine()) != null) {
            String prefixWithIdentifier = stru.split("\t")[1];
            String identifier = prefixWithIdentifier.split(":")[1];
            if (identifierToPrefixMap.get(identifier) == null) {
                identifierToPrefixMap.put(identifier, prefixWithIdentifier);
            } else {
                System.out.println("LOOKOUT!");
            }
        }

        //TODO: Should display name be updated? Technically the identifier includes the prefix, but from Reactome's perspective we've never included the prefix for any of the KEGG identifiers.

        // Now with the map, I get the reference database of interest. Then I get all referrals of it, which corresponds to the 178 DatabaseIdentifiers.
        GKInstance keggMycoDBInst = (GKInstance) dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase, ReactomeJavaConstants.name, "=", "KEGG Gene (Mycobacterium tuberculosis H37Rv)").iterator().next();
        Collection<GKInstance> keggMycoReferrals = keggMycoDBInst.getReferers(ReactomeJavaConstants.referenceDatabase);

        for (GKInstance keggMycoRefferal : keggMycoReferrals) {
            String identifier = keggMycoRefferal.getAttributeValue(ReactomeJavaConstants.identifier).toString();
            if (identifierToPrefixMap.get(identifier) !=null) {
                // DatabaseIdentifier has its 'identifier' attribute updated to include the prefix. Note: The displayName will remain unchanged.
                keggMycoRefferal.setAttributeValue(ReactomeJavaConstants.identifier, identifierToPrefixMap.get(identifier));
                keggMycoRefferal.getAttributeValue(ReactomeJavaConstants.modified);
                keggMycoRefferal.addAttributeValue(ReactomeJavaConstants.modified, instanceEditInst);
                dba.updateInstanceAttribute(keggMycoRefferal, ReactomeJavaConstants.identifier);
                dba.updateInstanceAttribute(keggMycoRefferal, ReactomeJavaConstants.modified);
            } else {
                System.out.println("THIS IS NOT A DRILL!");
            }
        }

        // Finally, update the access URL in the ReferenceDatabase, removing the 'mtv:' component.
        String accessUrl = keggMycoDBInst.getAttributeValue(ReactomeJavaConstants.accessUrl).toString();
        accessUrl = accessUrl.replace("mtv:", "");
        keggMycoDBInst.setAttributeValue(ReactomeJavaConstants.accessUrl, accessUrl);
        keggMycoDBInst.getAttributeValue(ReactomeJavaConstants.modified);
        keggMycoDBInst.addAttributeValue(ReactomeJavaConstants.modified, instanceEditInst);
        dba.updateInstanceAttribute(keggMycoDBInst, ReactomeJavaConstants.accessUrl);
        dba.updateInstanceAttribute(keggMycoDBInst, ReactomeJavaConstants.modified);

        System.out.println("Finished updating accessURL and identifiers associated with M. tuberculosis H37Rv");

    }
}
