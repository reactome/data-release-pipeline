package org.reactome.release;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collection;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BioModelsUtilities.class})
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "javax.script.*",
        "javax.xml.*", "com.sun.org.apache.xerces.*", "org.xml.sax.*", "com.sun.xml.*", "org.w3c.dom.*"})
public class BioModelsUtilitiesTester {

    @Mock
    private MySQLAdaptor mockAdaptor;
    @Mock
    private GKInstance mockBiomodelsDatabase;

    @Test
    public void bioModelsReferenceDatabaseExistsInDBReturnsInstance() throws Exception {
        Collection<GKInstance> collectionWithMockDatabaseInstance = new ArrayList<>();
        collectionWithMockDatabaseInstance.add(mockBiomodelsDatabase);
        Mockito.when(mockAdaptor.fetchInstanceByAttribute("ReferenceDatabase", "name", "=", "BioModels")).thenReturn(collectionWithMockDatabaseInstance);

        GKInstance returnedDbInstance = BioModelsUtilities.retrieveBioModelsDatabaseInstance(mockAdaptor);

        assertThat(returnedDbInstance, is(equalTo(mockBiomodelsDatabase)));
    }

    @Test
    public void bioModelsReferenceDatabaseDoesNotExistInDBReturnsNull() throws Exception {
        Collection<GKInstance> collectionWithoutMockDatabaseInstance = new ArrayList<>();
        Mockito.when(mockAdaptor.fetchInstanceByAttribute("ReferenceDatabase", "name", "=", "BioModels")).thenReturn(collectionWithoutMockDatabaseInstance);

        GKInstance returnedDbInstance = BioModelsUtilities.retrieveBioModelsDatabaseInstance(mockAdaptor);

        assertThat(returnedDbInstance, is(nullValue()));
    }

}
