package org.reactome.release.chebiupdate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.MySQLAdaptor.AttributeQueryRequest;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.Schema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.reactome.release.common.database.InstanceEditUtils;

import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.DataItem;
import uk.ac.ebi.chebi.webapps.chebiWS.model.Entity;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "uk.ac.ebi.*", "javax.management.*", "javax.script.*", 
			"javax.xml.*", "com.sun.org.apache.xerces.*", "org.xml.sax.*", "com.sun.xml.*", "org.w3c.dom.*", "org.mockito.*"})
@PrepareForTest({InstanceEditUtils.class, ChebiUpdater.class, ChebiDataRetriever.class})
public class ChebiUpdaterTest
{
	private static final long PERSON_ID = 12345L;

	@Mock(name="chebiClient")
	ChebiWebServiceClient chebiClient = PowerMockito.mock(ChebiWebServiceClient.class);
	
	@Mock(name = "adaptor")
	MySQLAdaptor adaptor;
	
	@Mock
	GKInstance chebiRefDB;
	
	@Mock
	GKInstance molecule1;
	
	@Mock
	GKInstance molecule2;

	@Mock
	GKInstance molecule3;

	@Mock
	GKInstance molecule4;
	
	@Mock
	GKInstance refEnt1;
	
	@Mock
	GKInstance refEnt2;
	
	@Mock
	GKInstance refEnt3;
	
	@Mock
	GKInstance refEnt4;

	@Mock
	GKInstance refEnt5;
	
	@Mock
	GKInstance refEnt6;
	
	// need to mock the Chebi Entities because they don't have a setter for Formulae
	@Mock
	Entity chebiEntity1;
	
	@Mock
	Entity chebiEntity2;
	
	@InjectMocks
	ChebiUpdater updator = new ChebiUpdater(adaptor, true, PERSON_ID, false);
	
	@Mock
	GKInstance person;
	
	@Mock
	ResultSet duplicateResults;
	
	@Mock
	Schema schema;
	
	@Mock
	GKSchemaClass refMolSchemaClass;
	
	@Mock
	GKSchemaClass simpleEntitySchemaClass;
	
	@Mock
	GKSchemaClass chemicalDrugSchemaClass;
	
	@Mock
	GKInstance createdInstance;
	
	@Mock
	GKSchemaAttribute attribute;
	
	@Before
	public void setupMolecule() throws InvalidAttributeException, Exception
	{
		MockitoAnnotations.initMocks(this);
		
		
		when(attribute.isMultiple()).thenReturn(false);
		
		when(refMolSchemaClass.getName()).thenReturn(ReactomeJavaConstants.ReferenceMolecule);
		when(simpleEntitySchemaClass.getName()).thenReturn(ReactomeJavaConstants.SimpleEntity);
		when(chemicalDrugSchemaClass.getName()).thenReturn(ReactomeJavaConstants.ChemicalDrug);
		PowerMockito.mockStatic(InstanceEditUtils.class);
		when(adaptor.fetchInstance(PERSON_ID)).thenReturn(person);
		
		when(adaptor.getSchema()).thenReturn(schema);

		when(schema.getClassByName(ReactomeJavaConstants.ReferenceMolecule)).thenReturn(refMolSchemaClass);
		when(person.getDbAdaptor()).thenReturn(adaptor);
		when(createdInstance.getAttributeValue(ReactomeJavaConstants.author)).thenReturn(person);
		
		when(InstanceEditUtils.createInstanceEdit(adaptor, PERSON_ID, updator.getClass().getCanonicalName())).thenReturn(person);
		
		when(chebiRefDB.getDBID()).thenReturn(123456L);
		when(adaptor.fetchInstanceByAttribute("ReferenceDatabase", "name", "=", "ChEBI")).thenReturn(Arrays.asList(chebiRefDB)) ;
		PowerMockito.whenNew(ChebiWebServiceClient.class).withAnyArguments().thenReturn(chebiClient);
	}
	
	@Test
	public void testUpdateMolecules()
	{
		try
		{
			
			when(chebiEntity1.getChebiAsciiName()).thenReturn("NAME1");
			when(chebiEntity2.getChebiAsciiName()).thenReturn("NAME-2");
			
			when(chebiEntity1.getChebiId()).thenReturn("CHEBI:112233");
			when(chebiEntity2.getChebiId()).thenReturn("CHEBI:332211-2");
			
			DataItem d = new DataItem();
			d.setData("H2");
			when(chebiEntity1.getFormulae()).thenReturn(Arrays.asList( d ));
			when(chebiEntity2.getFormulae()).thenReturn(Arrays.asList( d ));
			
			when(chebiClient.getCompleteEntity("112233")).thenReturn(chebiEntity1);
			when(chebiClient.getCompleteEntity("332211")).thenReturn(chebiEntity2);
			when(chebiClient.getCompleteEntity("445566")).thenReturn(null);
			when(chebiClient.getCompleteEntity("778899")).thenReturn(chebiEntity1);
			
			when(molecule1.getAttributeValue("identifier")).thenReturn("112233");
			when(molecule1.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("NAME1")));
			when(molecule1.getAttributeValue("formula")).thenReturn("H3");
			when(molecule1.getDBID()).thenReturn(123L);
			when(molecule1.getSchemClass()).thenReturn(refMolSchemaClass);
			when(adaptor.fetchInstance(123L)).thenReturn(molecule1);
			
			when(molecule2.getAttributeValue("identifier")).thenReturn("332211");
			when(molecule2.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("NAME2")));
			when(molecule2.getDBID()).thenReturn(456L);
			when(molecule2.getSchemClass()).thenReturn(refMolSchemaClass);
			when(adaptor.fetchInstance(456L)).thenReturn(molecule2);
			
			when(molecule3.getAttributeValue("identifier")).thenReturn("445566");
			
			when(molecule4.getAttributeValue("identifier")).thenReturn("778899");
			when(molecule4.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("NAME1")));
			when(molecule4.getAttributeValue("formula")).thenReturn("H2");
			when(molecule4.getSchemClass()).thenReturn(refMolSchemaClass);
			when(molecule4.getDBID()).thenReturn(444L);
			when(adaptor.fetchInstance(444L)).thenReturn(molecule4);
			
			when(refEnt1.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("OTHERNAME", "NAME1", "BLAH")));
			when(refEnt1.getSchemClass()).thenReturn(simpleEntitySchemaClass);
			when(refEnt2.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("TEST", "NAME2")));
			when(refEnt2.getSchemClass()).thenReturn(simpleEntitySchemaClass);
			when(refEnt3.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("NAME-2", "ASDFQ@#$FASFASDF","NOPE")));
			when(refEnt3.getSchemClass()).thenReturn(chemicalDrugSchemaClass);
			
			when(refEnt1.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstance);
			when(refEnt2.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstance);
			when(refEnt3.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstance);
			
			Collection<GKInstance> referrers1 = Arrays.asList(refEnt1);
			Collection<GKInstance> referrers2 = Arrays.asList(refEnt2,refEnt3); 
			
			when(molecule1.getReferers("referenceEntity")).thenReturn(referrers1);
			when(molecule2.getReferers("referenceEntity")).thenReturn(referrers2);
			Collection<GKInstance> molecules = Arrays.asList(molecule1, molecule2, molecule3, molecule4);
			GKInstance createdInstEd = Mockito.mock(GKInstance.class);
			when(createdInstEd.getAttributeValue(ReactomeJavaConstants.author)).thenReturn(person);
			for (GKInstance molecule : molecules)
			{
				when(molecule.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstEd);
			}
			for (GKInstance ref : referrers1)
			{
				when(ref.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstEd);
			}
			for (GKInstance ref : referrers2)
			{
				when(ref.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstEd);
			}
			when(adaptor.fetchInstanceByAttribute("ReferenceMolecule", "referenceDatabase", "=", "123456")).thenReturn(molecules);
			
			updator.updateChebiReferenceMolecules();
		}
		catch (Exception e)
		{
			fail(e.getMessage());
			throw new RuntimeException(e);
		}
		// If we got this far, it means nothing crashed.
		assertTrue(true);
	}
	
	@Test
	public void testChangedIdentifiers() throws InvalidAttributeException, Exception
	{
		when(chebiEntity1.getChebiAsciiName()).thenReturn("NAME1");
		// This mock ChEBI entity will return a different ChEBI ID! It's ID is now the same as molecule2.
		when(chebiEntity1.getChebiId()).thenReturn("CHEBI:332211");
		
		when(chebiEntity2.getChebiAsciiName()).thenReturn("NAME-2");
		when(chebiEntity2.getChebiId()).thenReturn("CHEBI:332211");
		
		when(chebiClient.getCompleteEntity("112233")).thenReturn(chebiEntity1);
		when(chebiClient.getCompleteEntity("332211")).thenReturn(chebiEntity2);

		DataItem d = new DataItem();
		d.setData("H2");
		when(chebiEntity1.getFormulae()).thenReturn(Arrays.asList( d ));
		when(chebiEntity2.getFormulae()).thenReturn(Arrays.asList( d ));

		when(molecule1.getAttributeValue("identifier")).thenReturn("112233");
		when(molecule1.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("NAME1")));
		when(molecule1.getAttributeValue("formula")).thenReturn("H3");
		when(molecule1.getDBID()).thenReturn(123L);
		when(molecule1.getSchemClass()).thenReturn(refMolSchemaClass);
		when(adaptor.fetchInstance(123L)).thenReturn(molecule1);
		
		when(molecule2.getAttributeValue("identifier")).thenReturn("332211");
		when(molecule2.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("NAME2")));
		when(molecule2.getDBID()).thenReturn(456L);
		when(molecule2.getSchemClass()).thenReturn(refMolSchemaClass);
		when(adaptor.fetchInstance(456L)).thenReturn(molecule2);

		
		when(refEnt1.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("OTHERNAME", "NAME1", "BLAH")));
		when(refEnt1.getSchemClass()).thenReturn(simpleEntitySchemaClass);
		when(refEnt1.getDBID()).thenReturn(1111L);
		when(refEnt2.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("TEST", "NAME2")));
		when(refEnt2.getSchemClass()).thenReturn(simpleEntitySchemaClass);
		when(refEnt2.getDBID()).thenReturn(2222L);
		when(refEnt3.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("NAME-2", "ASDFQ@#$FASFASDF","NOPE")));
		when(refEnt3.getDBID()).thenReturn(3333L);
		when(refEnt4.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("BLAH4", "more blah")));
		when(refEnt4.getDBID()).thenReturn(4444L);
		when(refEnt5.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("5BLAH", "more blah")));
		when(refEnt5.getDBID()).thenReturn(5555L);
		when(refEnt6.getAttributeValuesList("name")).thenReturn(new ArrayList<>(Arrays.asList("BL_6_AH", "more blah")));
		when(refEnt6.getDBID()).thenReturn(6666L);

		when(refEnt1.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstance);
		when(refEnt2.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstance);
		when(refEnt3.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstance);
		when(refEnt5.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstance);
		when(refEnt6.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstance);

		Collection<GKInstance> referrers1 = Arrays.asList(refEnt1, refEnt4, refEnt6);
		Collection<GKInstance> referrers2 = Arrays.asList(refEnt2, refEnt3, refEnt5); 
		
		when(molecule1.getReferers("referenceEntity")).thenReturn(referrers1);
		when(molecule2.getReferers("referenceEntity")).thenReturn(referrers2);
		Collection<GKInstance> molecules = Arrays.asList(molecule1, molecule2);

		GKInstance createdInstEd = Mockito.mock(GKInstance.class);
		when(createdInstEd.getAttributeValue(ReactomeJavaConstants.author)).thenReturn(person);
		for (GKInstance molecule : molecules)
		{
			when(molecule.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstEd);
		}
		for (GKInstance ref : referrers1)
		{
			when(ref.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstEd);
		}
		for (GKInstance ref : referrers2)
		{
			when(ref.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstEd);
		}
		
		when(adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule, ReactomeJavaConstants.referenceDatabase, "=", "123456")).thenReturn(molecules);
		//adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule, ReactomeJavaConstants.identifier, "=", newChebiID);
		when(adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule, ReactomeJavaConstants.identifier, "=", "332211")).thenReturn(Arrays.asList(molecule2));
		updator.updateChebiReferenceMolecules();
		// If we got this far, it means nothing crashed!
		assertTrue(true);
	}
	
	@Test
	public void testCheckForDuplicates() throws SQLException, Exception
	{
		when(adaptor.executeQuery(anyString(), (List<?>) nullable(List.class))).thenReturn(duplicateResults);
		when(duplicateResults.next()).thenReturn(true).thenReturn(true).thenReturn(false);
		when(duplicateResults.getString(1)).thenReturn("123").thenReturn("456");
		when(duplicateResults.getInt(2)).thenReturn(2).thenReturn(3);
		AttributeQueryRequest mockAQR = Mockito.mock(AttributeQueryRequest.class);
		PowerMockito.whenNew(AttributeQueryRequest.class).withAnyArguments().thenReturn(mockAQR);
		
		Set<GKInstance> result1 = new HashSet<>();
		result1.add(molecule1);
		Set<GKInstance> result2 = new HashSet<>();
		result2.add(molecule2);
		
		Collection<GKInstance> molecules = Arrays.asList(molecule1, molecule2);

		GKInstance createdInstEd = Mockito.mock(GKInstance.class);
		when(createdInstEd.getAttributeValue(ReactomeJavaConstants.author)).thenReturn(person);
		for (GKInstance molecule : molecules)
		{
			when(molecule.getAttributeValue(ReactomeJavaConstants.created)).thenReturn(createdInstEd);
		}
		
		when(adaptor._fetchInstance(ArgumentMatchers.anyList())).thenReturn( result1 ).thenReturn( result2 );
		
		updator.checkForDuplicates();
	}
}
