package org.reactome.orthoinference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.SchemaClass;

public class OrthologousEntity {
	
	private static HashMap<GKInstance, GKInstance> orthologousEntity = new HashMap<GKInstance,GKInstance>();
	private static HashMap<GKInstance, GKInstance> homolGEE = new HashMap<GKInstance,GKInstance>();
	private static HashMap<GKInstance, GKInstance> complexPolymer = new HashMap<GKInstance, GKInstance>();
	private static MySQLAdaptor dba;
	static GenerateInstance createInferredInstance = new GenerateInstance();
	static GKInstance nullInst = null;
	
	public void setAdaptor(MySQLAdaptor dbAdaptor)
	{
		OrthologousEntity.dba = dbAdaptor;
	}
	
	private static GKInstance speciesInst = null;
	
	// Sets the species instance for inferEWAS to use
	public void setSpeciesInst(GKInstance speciesInstCopy)
	{
		speciesInst = speciesInstCopy;
	}

	public static GKInstance createOrthoEntity(GKInstance entityInst, boolean override) throws InvalidAttributeException, Exception
	{
		if (override)
		{
			System.out.println("      " + entityInst.getAttributeValue("DB_ID"));
		}
		GKInstance infEntity = null;
		if (entityInst.getSchemClass().isValidAttribute("species"))
		{
			// TODO: Make sure this null check actually works
			if (orthologousEntity.get(entityInst) == null)
			{
				// TODO: Verify this works for all instance types
				if (!OrthologousEntity.hasSpecies(entityInst))
				{
					infEntity = entityInst;
				} else if (entityInst.getSchemClass().isa("GenomeEncodedEntity"))
				{
					// Change to 'EWAS' and make the else create ghost
					if (entityInst.getSchemClass().toString().contains("GenomeEncodedEntity"))
					{
						if (override)
						{
							GKInstance mockedInst = GenerateInstance.newMockGKInstance(entityInst);
							return mockedInst;
						}
					} else {
						System.out.println("   GEE");
						infEntity = OrthologousEntity.createInfGEE(entityInst, override);
					}
				} else if (entityInst.getSchemClass().isa("Complex") || entityInst.getSchemClass().isa("Polymer"))
				{
					System.out.println("   Complex/Polymer");
					infEntity = OrthologousEntity.createInfComplexPolymer(entityInst, override);
				} else if (entityInst.getSchemClass().isa("EntitySet"))
				{
					//TODO: Is this species check redundant? Depends if EntitySet can have it while it's constituent parts might not, and vice versa
					if (entityInst.getAttributeValue("species") != null)
					{
						System.out.println("   EntitySet");
						infEntity = OrthologousEntity.createInfEntitySet(entityInst, override);
					} else {
						infEntity = entityInst;
					}
				} else if (entityInst.getSchemClass().isa("SimpleEntity"))
				{
					infEntity = entityInst;
				} else {
					//TODO: Unknown Class
				}
				//TODO: %orthologous_entity
				if (override)
				{
					return infEntity;
				}
				
				orthologousEntity.put(entityInst, infEntity);
			} 
			return orthologousEntity.get(entityInst);
		} else {
			//TODO: check intracellular; if flag create clone;
			System.out.println("Invalid");
			return entityInst;
		}
	}
	
	// Determines if there is a species attribute in any constituent instances of entityInst
	// Unless its an 'OtherEntity', the function will check the instance or iterate on  it's
	// sub-instances until it finds an existing 'species' attribute, or else it will return false.
	public static boolean hasSpecies(GKInstance entityInst) throws InvalidAttributeException, Exception
	{
		if (entityInst.getSchemClass().isa(ReactomeJavaConstants.OtherEntity))
		{
			return false;
		} else if (entityInst.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) // || entityInst.getSchemClass().isa(ReactomeJavaConstants.Polymer) || entityInst.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
		{
			for (Object member : entityInst.getAttributeValuesList(ReactomeJavaConstants.hasMember))
			{
				if (OrthologousEntity.hasSpecies((GKInstance) member))
				{
					return true;
				}
			}
			if (entityInst.getSchemClass().isa(ReactomeJavaConstants.CandidateSet)) {
				for (Object candidate : entityInst.getAttributeValuesList(ReactomeJavaConstants.hasCandidate))
				{
					if (OrthologousEntity.hasSpecies((GKInstance) candidate))
					{
						return true;
					}
				}
			}
			return false;
		} else if (entityInst.getSchemClass().isa(ReactomeJavaConstants.Complex))
		{
			for (Object component : entityInst.getAttributeValuesList(ReactomeJavaConstants.hasComponent))
			{
				if (OrthologousEntity.hasSpecies((GKInstance) component))
				{
					return true;
				}
			}
			return false;
		} else if (entityInst.getSchemClass().isa(ReactomeJavaConstants.Polymer))
		{
			for (Object monomer : entityInst.getAttributeValuesList(ReactomeJavaConstants.repeatedUnit))
			{
				if (OrthologousEntity.hasSpecies((GKInstance) monomer))
				{
					return true;
				}
			}
			return false;
		} else {
			if (entityInst.getAttributeValue(ReactomeJavaConstants.species) != null)
			{
				return true;
			} else {
				return false;
			}
		}
	}
	// TODO: Naming change (??); Function description
	public static GKInstance createInfGEE(GKInstance geeInst, boolean override) throws InvalidAttributeException, Exception
	{
		if (homolGEE.get(geeInst) == null)
		{
			ArrayList<GKInstance> infEWASInstances = InferEWAS.inferEWAS(geeInst);
			if (infEWASInstances.size() > 1)
			{
				// TODO: Instance Edit; Check Intracellular; $opt_filt (??); add attribute values if necessary - inferredFrom/To
				SchemaClass definedSetClass = dba.getSchema().getClassByName(ReactomeJavaConstants.DefinedSet);
				GKInstance definedSetInst = new GKInstance(definedSetClass);
				definedSetInst.setDbAdaptor(dba);
				String definedSetName = "Homologues of " + geeInst.getAttributeValue("name");
				definedSetInst.addAttributeValue(ReactomeJavaConstants.name, definedSetName);
				definedSetInst.addAttributeValue(ReactomeJavaConstants.species, speciesInst);
				definedSetInst.addAttributeValue(ReactomeJavaConstants.hasMember, infEWASInstances);
				definedSetInst = GenerateInstance.checkForIdenticalInstances(definedSetInst);
				homolGEE.put(geeInst, definedSetInst);
			} else if (infEWASInstances.size() == 1)
			{
				homolGEE.put(geeInst, infEWASInstances.get(0));
			} else {
				if (override) {
				GKInstance mockedInst = GenerateInstance.newMockGKInstance(geeInst);
				return mockedInst;
				} else {
					return nullInst;
				}
			}
		}
		return homolGEE.get(geeInst);
	}
	
	public static GKInstance createInfComplexPolymer(GKInstance complexInst, boolean override) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		if (complexPolymer.get(complexInst) == null)
		{
			//TODO: %inferred_cp; count distinct proteins; filter based on returned protein count and threshold
			GKInstance infComplexInst = GenerateInstance.newInferredGKInstance(complexInst);
			
			List<Integer> complexProteinCounts = ProteinCount.countDistinctProteins(complexInst);
			System.out.println("Complex Counts: " + complexProteinCounts);
			int complexTotal = complexProteinCounts.get(0);
			int complexInferred = complexProteinCounts.get(1);
			int complexMax = complexProteinCounts.get(2);
			
			int percent = 0;
			if (complexTotal > 0)
			{
				percent = (complexInferred * 100)/complexTotal;
			}
			if (!override)
			{
				if (complexTotal > 0 && complexInferred == 0)
				{
					return nullInst;
				}
				if (percent < 75)
				{
					return nullInst;
				}
			}

			GKInstance complexSummation = new GKInstance(dba.getSchema().getClassByName(ReactomeJavaConstants.Summation));
			//TODO: Remove brackets from name
			infComplexInst.addAttributeValue(ReactomeJavaConstants.name, complexInst.getAttributeValue("name"));
			complexSummation.addAttributeValue(ReactomeJavaConstants.text, "This complex/polymer has been computationally inferred (based on Ensembl Compara) from a complex/polymer involved in an event that has been demonstrated in another species.");
			complexSummation = GenerateInstance.checkForIdenticalInstances(complexSummation);
			infComplexInst.addAttributeValue(ReactomeJavaConstants.summation, complexSummation);
			//TODO: check for identical instances (complexSummation)
			ArrayList<GKInstance> infComponents = new ArrayList<GKInstance>();
			if (complexInst.getSchemClass().isa(ReactomeJavaConstants.Complex))
			{
				for (Object componentInst : complexInst.getAttributeValuesList(ReactomeJavaConstants.hasComponent))
				{		
					infComponents.add(OrthologousEntity.createOrthoEntity((GKInstance) componentInst, true));
				}
			infComplexInst.addAttributeValue(ReactomeJavaConstants.hasComponent, infComponents);
			} else {
				for (Object componentInst : complexInst.getAttributeValuesList(ReactomeJavaConstants.repeatedUnit))
				{		
					infComponents.add(OrthologousEntity.createOrthoEntity((GKInstance) componentInst, true));
				}
			infComplexInst.addAttributeValue(ReactomeJavaConstants.repeatedUnit, infComponents);
			}
			//TODO: Add total,inferred,max proteins count; inferredTo & inferredFrom; update; add to hash
			if (override)
			{
				return infComplexInst;
			} else {
				complexPolymer.put(complexInst, infComplexInst);
			}
		}
		return complexPolymer.get(complexInst);
	}
	//TODO: The organization of this function could probably be re-organized
	public static GKInstance createInfEntitySet(GKInstance attributeInst, boolean override) throws InvalidAttributeException, Exception
	{
		//TODO: %inferred_gse; [infer members] proper filtering; Could infer_members happen after protein count?
		HashSet<String> existingMembers = new HashSet<String>();
		ArrayList<GKInstance> membersList = new ArrayList<GKInstance>();
		// Equivalent to infer_members
		System.out.println("Member?");
		for (Object memberInst : attributeInst.getAttributeValuesList(ReactomeJavaConstants.hasMember))
		{
			GKInstance infMember = OrthologousEntity.createOrthoEntity((GKInstance) memberInst, false);
			if (infMember != null)
			{
				existingMembers.add(infMember.getAttributeValue("name").toString());
				membersList.add(infMember);
			}
		}
		GKInstance infEntitySetInst = GenerateInstance.newInferredGKInstance(attributeInst);
		infEntitySetInst.addAttributeValue(ReactomeJavaConstants.name, attributeInst.getAttributeValue("name"));
		infEntitySetInst.addAttributeValue(ReactomeJavaConstants.hasMember, membersList);
		if (attributeInst.getSchemClass().isa(ReactomeJavaConstants.OpenSet))
		{
			infEntitySetInst.addAttributeValue(ReactomeJavaConstants.referenceEntity, attributeInst.getAttributeValue("referenceEntity"));
		} else {
			//TODO: Count distinct proteins;
			List<Integer> entitySetProteinCounts = ProteinCount.countDistinctProteins(attributeInst);
			System.out.println("EntitySet Counts:" + entitySetProteinCounts);
			int entitySetTotal = entitySetProteinCounts.get(0);
			int entitySetInferred = entitySetProteinCounts.get(1);
			int entitySetMax = entitySetProteinCounts.get(2);
			if (!override && entitySetTotal > 0 && entitySetInferred == 0)
			{
				return infEntitySetInst;
			}
			if (attributeInst.getSchemClass().isa(ReactomeJavaConstants.CandidateSet))
			{
				HashSet<String> existingCandidates = new HashSet<String>();
				ArrayList<GKInstance> candidatesListUnfiltered = new ArrayList<GKInstance>();
				// Equivalent to infer_members
				for (Object candidateInst : attributeInst.getAttributeValuesList(ReactomeJavaConstants.hasCandidate))
				{
					GKInstance infCandidate = OrthologousEntity.createOrthoEntity((GKInstance) candidateInst, false);
					if (infCandidate != null)
					{
						existingCandidates.add(infCandidate.getAttributeValue("name").toString());
						candidatesListUnfiltered.add(infCandidate);
					}
				}
				// Check for duplicate instances between membersList and candidatesList, keeping only unique ones	
				ArrayList<GKInstance> candidatesList = new ArrayList<GKInstance>();
				for (GKInstance candidate : candidatesListUnfiltered)
				{
					int memberCount = 0;
					for (GKInstance member : membersList)
					{
						if (candidate == member)
						{
							memberCount++;
						}
					}
					if (memberCount == 0)
					{
						candidatesList.add(candidate);
					}
				}
				if (candidatesList.size() == 1)
				{
					infEntitySetInst.addAttributeValue(ReactomeJavaConstants.hasCandidate, candidatesList);
				} else {
					if (membersList.size() != 0)
					{
						if (membersList.size() == 1)
						{
							infEntitySetInst = membersList.get(0);
						} else {
							SchemaClass definedSetClass = dba.getSchema().getClassByName(ReactomeJavaConstants.DefinedSet);
							GKInstance definedSetInst = new GKInstance(definedSetClass);
							definedSetInst.setDbAdaptor(dba);
							definedSetInst.setAttributeValue(ReactomeJavaConstants.name, infEntitySetInst.getAttributeValuesList(ReactomeJavaConstants.name));
							definedSetInst.setAttributeValue(ReactomeJavaConstants.hasMember, membersList);
							infEntitySetInst = definedSetInst;
						}
					} else {
						if (override)
						{
							GKInstance mockedEntitySetInst = GenerateInstance.newMockGKInstance(attributeInst);
							return mockedEntitySetInst;
						} else {
							return nullInst;
						}
					}
				}
				
			} else if (attributeInst.getSchemClass().isa(ReactomeJavaConstants.DefinedSet))
			{
				if (membersList.size() == 0)
				{
					if (override)
					{
						GKInstance mockedEntitySetInst = GenerateInstance.newMockGKInstance(attributeInst);
						return mockedEntitySetInst;
					} else {
						return nullInst;
					}
				} else if (membersList.size() == 1) {
					//TODO: Make inferred instance that member
				}
				// If it has more than 1 member, nothing happens here, as all members are in this inferred instances 'HasMember' attribute
			}
		}
		//TODO: Check for identical instances
		if (infEntitySetInst.getSchemClass().isValidAttribute("species") && attributeInst.getAttributeValue("species") != null)
		{
			// add attribute value if necessary InferredFrom/To; update; %inferred_gse
		}
		return infEntitySetInst;
		
	}
}