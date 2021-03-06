package org.reactome.release.updateDOIs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
import org.reactome.release.updateDOIs.UpdateDOIs;

//@PrepareForTest({UpdateDOIs.class})
public class TestUpdateDOIs {

//@PrepareForTest({UpdateDOIs.class})
	private MySQLAdaptor mockAdaptor = PowerMockito.mock(MySQLAdaptor.class);
	private static final Path MOCK_REPORT_PATH = Paths.get("reportPath");
	private static final int MOCK_RELEASE_NUMBER = 70;
	private static final long MOCK_PERSON_DBID = 12345L;
	
	@Before
	public void setup() throws Exception
	{
		PowerMockito.whenNew(MySQLAdaptor.class).withAnyArguments().thenReturn(mockAdaptor);
	}
	
//	@Test
//	public void testUpdateDOIs() throws Exception
//	{
//		UpdateDOIs check = new UpdateDOIs();
//	}
	
	@Test
	public void testfindNewDOIsAndUpdate() throws Exception 
	{
		UpdateDOIs check = new UpdateDOIs();
		check.setAdaptors(mockAdaptor, mockAdaptor);
		
		GKInstance inst1 = PowerMockito.mock(GKInstance.class);
		
		Collection<GKInstance> testResults = Arrays.asList(inst1);
		PowerMockito.when((GKInstance) inst1.getAttributeValue("stableIdentifier")).thenReturn(inst1);
		PowerMockito.when(inst1.getDisplayName()).thenReturn("HSA-12345");
		
		PowerMockito.when(inst1.getAttributeValue("name")).thenReturn("Name Value");
		PowerMockito.when(inst1.getAttributeValue("DB_ID")).thenReturn("67890");
		
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "doi", "NOT REGEXP", "^10.3180")).thenReturn(testResults);
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "DB_ID", "=", "67890")).thenReturn(testResults);

		check.findAndUpdateDOIs(MOCK_PERSON_DBID, MOCK_REPORT_PATH, MOCK_RELEASE_NUMBER, true);
    }
	
	@Test
	public void testfindNewDOIsAndUpdateEmptyList() throws Exception 
	{
		UpdateDOIs check = new UpdateDOIs();
		check.setAdaptors(mockAdaptor, mockAdaptor);
		
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "doi", "NOT REGEXP", "^10.3180")).thenReturn(new ArrayList<GKInstance>());
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "DB_ID", "=", "67890")).thenReturn(new ArrayList<GKInstance>());
		
		check.findAndUpdateDOIs(MOCK_PERSON_DBID, MOCK_REPORT_PATH, MOCK_RELEASE_NUMBER, true);
	}
	
	@Test
	public void testfindNewDOIsAndUpdateDeepEmptyList() throws Exception 
	{
		UpdateDOIs check = new UpdateDOIs();
		check.setAdaptors(mockAdaptor, mockAdaptor);
		
		GKInstance inst1 = PowerMockito.mock(GKInstance.class);
		Collection<GKInstance> testResults = Arrays.asList(inst1);
		
		PowerMockito.when((GKInstance) inst1.getAttributeValue("stableIdentifier")).thenReturn(inst1);
		PowerMockito.when(inst1.getAttributeValue("name")).thenReturn("Name Value");
		PowerMockito.when(inst1.getAttributeValue("DB_ID")).thenReturn("67890");
		
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "doi", "NOT REGEXP", "^10.3180")).thenReturn(testResults);
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "DB_ID", "=", "67890")).thenReturn(new ArrayList<GKInstance>());
		
		check.findAndUpdateDOIs(MOCK_PERSON_DBID, MOCK_REPORT_PATH, MOCK_RELEASE_NUMBER,true);
	}
}