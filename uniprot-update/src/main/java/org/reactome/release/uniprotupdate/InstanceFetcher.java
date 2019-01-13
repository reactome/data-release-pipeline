package org.reactome.release.uniprotupdate;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

import java.util.*;

import static org.reactome.release.uniprotupdate.UniprotConstants.*;

public class InstanceFetcher {
    private static Map<MySQLAdaptor, InstanceFetcher> instanceFetcherMap;

    private GKInstance ensemblHSapiensRefDB;
    private GKInstance uniprotRefDB;
    private GKInstance humanSpecies;

    private InstanceFetcher(MySQLAdaptor adaptor) {
        try {
            initializeInstances(adaptor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeInstances(MySQLAdaptor adaptor) throws Exception {
        ensemblHSapiensRefDB = getInstanceByName(adaptor, ReactomeJavaConstants.ReferenceDatabase,
            ENSEMBL_HOMO_SAPIENS_GENE);
        humanSpecies = getInstanceByName(adaptor, ReactomeJavaConstants.Species, HOMO_SAPIENS);
        uniprotRefDB = getInstanceByName(adaptor, ReactomeJavaConstants.ReferenceDatabase, UNIPROT);
    }

    public static synchronized InstanceFetcher getInstanceFetcher(MySQLAdaptor adaptor) {
        if (instanceFetcherMap == null) {
            instanceFetcherMap = new HashMap<>();
        }

        InstanceFetcher instanceFetcher = instanceFetcherMap.get(adaptor);
        if (instanceFetcher == null) {
            instanceFetcher = new InstanceFetcher(adaptor);
            instanceFetcherMap.put(adaptor, instanceFetcher);
        }

        return instanceFetcher;
    }

    public GKInstance getEnsemblHSapiensRefDB() {
        return ensemblHSapiensRefDB;
    }

    public GKInstance getHumanSpecies() {
        return humanSpecies;
    }

    public GKInstance getUniprotRefDB() {
        return uniprotRefDB;
    }

    private GKInstance getInstanceByName(MySQLAdaptor adaptor, String schemaClass, String name)
            throws Exception {
        @SuppressWarnings("unchecked")
        List<GKInstance> instances = new ArrayList<>((Collection<GKInstance>) adaptor.fetchInstanceByAttribute(
            schemaClass,
            ReactomeJavaConstants.name,
            "=",
            name
        ));
        return instances.get(0);
    }
}
