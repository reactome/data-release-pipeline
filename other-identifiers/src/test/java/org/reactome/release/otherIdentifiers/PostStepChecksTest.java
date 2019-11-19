package org.reactome.release.otherIdentifiers;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
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

import static org.junit.jupiter.api.Assertions.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PostStepChecks.class})
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "javax.script.*",
        "javax.xml.*", "com.sun.org.apache.xerces.*", "org.xml.sax.*", "com.sun.xml.*", "org.w3c.dom.*", "org.mockito.*"})
public class PostStepChecksTest {

    private final long correctOtherIdentifierCount = 1L;

    @Mock
    GKInstance mockRGPInstance;
    @Mock
    GKInstance mockRGPInstance2;

    Collection<GKInstance> rgpInstanceCollection = new ArrayList<>();
    List<String> otherIdentifierList = new ArrayList<>();
    List<String> otherIdentifierList2 = new ArrayList<>();

    @Test
    public void getOtherIdentifierCountReturnsCorrectOtherIdentifierCount() throws Exception {

        rgpInstanceCollection.add(mockRGPInstance);
        rgpInstanceCollection.add(mockRGPInstance2);
        otherIdentifierList.add("Mock otherIdentifier");
        Mockito.when(mockRGPInstance.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier)).thenReturn(otherIdentifierList);
        Mockito.when(mockRGPInstance2.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier)).thenReturn(otherIdentifierList2);
        long otherIdentifierCount = PostStepChecks.getCountOfInstancesWithOtherIdentifiers(rgpInstanceCollection);
        assertThat(otherIdentifierCount, is(equalTo(correctOtherIdentifierCount)));
    }

    @Test
    public void instanceWithoutOtherIdentifiersReturnsFalse() throws Exception {
        Mockito.when(mockRGPInstance.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier)).thenReturn(otherIdentifierList);
        boolean hasOtherIdentifiers = PostStepChecks.hasOtherIdentifiers(mockRGPInstance);
        assertThat(hasOtherIdentifiers, is(equalTo(false)));
    }

    @Test
    public void instanceWithOtherIdentifiersReturnsTrue() throws Exception {
        rgpInstanceCollection.add(mockRGPInstance);
        otherIdentifierList.add("Mock otherIdentifier");
        Mockito.when(mockRGPInstance.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier)).thenReturn(otherIdentifierList);
        boolean hasOtherIdentifiers = PostStepChecks.hasOtherIdentifiers(mockRGPInstance);
        assertThat(hasOtherIdentifiers, is(equalTo(true)));
    }

    @Test
    public void getCountOfInstancesWithOtherIdentifiersNullTest() {
        NullPointerException thrown = assertThrows(
                NullPointerException.class,
                () -> PostStepChecks.getCountOfInstancesWithOtherIdentifiers(null));
        assertThat(thrown.toString(), containsString("NullPointerException"));
    }

    @Test
    public void hasOtherIdentifiersNullTest() {
            RuntimeException thrown = assertThrows(
                    RuntimeException.class,
                    () -> PostStepChecks.hasOtherIdentifiers(null));
            assertThat(thrown.getMessage(), containsString("Unable to retrieve other identifiers from RGP instance"));
    }

    @Test
    public void emptyCollectionReturnsZero() {
        long otherIdentifierCount = PostStepChecks.getCountOfInstancesWithOtherIdentifiers(rgpInstanceCollection);
        assertThat(otherIdentifierCount, is(equalTo(0L)));
    }

    @Test
    public void instanceWithMultipleOtherIdentifiersCountedCorrectly() throws Exception {
        rgpInstanceCollection.add(mockRGPInstance);
        otherIdentifierList.add("Mock otherIdentifier");
        otherIdentifierList.add("Mock otherIdentifier 2");
        Mockito.when(mockRGPInstance.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier)).thenReturn(otherIdentifierList);
        long otherIdentifierCount = PostStepChecks.getCountOfInstancesWithOtherIdentifiers(rgpInstanceCollection);
        assertThat(otherIdentifierCount, is(equalTo(correctOtherIdentifierCount)));
    }
}
