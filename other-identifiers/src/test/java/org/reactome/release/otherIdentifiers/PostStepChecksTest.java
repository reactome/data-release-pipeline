package org.reactome.release.otherIdentifiers;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PostStepChecks.class})
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "javax.script.*",
        "javax.xml.*", "com.sun.org.apache.xerces.*", "org.xml.sax.*", "com.sun.xml.*", "org.w3c.dom.*", "org.mockito.*"})
public class PostStepChecksTest {

    private final int correctOtherIdentifierCount = 2;

    @Mock
    GKInstance mockGKInstance;

    Collection<GKInstance> gkInstanceCollection = new ArrayList<>();
    List<Object> otherIdentifierCollection = new ArrayList<>();

    @Test
    public void getOtherIdentifierCountReturnsAtomicInt() throws Exception {

        gkInstanceCollection.add(mockGKInstance);
        gkInstanceCollection.add(mockGKInstance);
        otherIdentifierCollection.add("Mock otherIdentifier");
        Mockito.when(mockGKInstance.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier)).thenReturn(otherIdentifierCollection);
        int otherIdentifierCount = PostStepChecks.getOtherIdentifierCount(gkInstanceCollection);
        assertThat(otherIdentifierCount, is(equalTo(correctOtherIdentifierCount)));
    }
}
