package org.reactome.release.uniprotupdate.instanceupdate;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.util.*;
import java.util.stream.Collectors;

public class SpeciesAttributeUpdater extends AttributeUpdater {
	private static Map<String, List<GKInstance>> speciesCache = new HashMap<>();

	SpeciesAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected String extractAttributeDataFromUniprot(UniprotData data) {
		return data.getOrganismName() != null ? data.getOrganismName() : "";
	}

	@Override
	public void updateAttribute(GKInstance instance, UniprotData data) {
		// The list of Species that we got by looking up the name from "data" does not
		// contain the Species DB ID on the current instance.
		// This means we need to update the instance to use the one from the input.
		// does it make sense in the data model for speciesInst to be null?
		GKInstance speciesInst = getSpeciesInstance(instance);
		if (speciesInst != null && !speciesDBIDs.contains(speciesInst.getDBID())) {
			instance.setAttributeValue(ReactomeJavaConstants.species, dataSpeciesInst.get(0));
			instance.getDbAdaptor().updateInstanceAttribute(instance, ReactomeJavaConstants.species);
		}
	}

	private GKInstance getSpeciesInstance(GKInstance instance) {
		try {
			return (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Set<Long> getSpeciesDbIds(String speciesName, MySQLAdaptor adaptor) throws Exception {
		return determineDataSpeciesInsts(speciesName, adaptor)
			.stream()
			.map(GKInstance::getDBID)
			.collect(Collectors.toSet());
	}

	/**
	 * Produces a list of Species Instances whose name matches a name given as an argument.
	 * Also maintains an internal cache of species, keyed by name. Use this method when you want cached species.
	 * @param speciesName - the species name to look up in the cache.
	 * @return A list of Species Instances whose name matches <code>speciesName</code>.
	 * It will probably only have 1 item in it.
	 * @throws Exception
	 */
	private List<GKInstance> determineDataSpeciesInsts(String speciesName, MySQLAdaptor adaptor) throws Exception {
		if (!speciesCache.containsKey(speciesName)) {
			speciesCache.put(speciesName, fetchSpeciesInstances(speciesName, adaptor));
			logger.info("Species cache miss on \"{}\"", speciesName);
		}
		return speciesCache.get(speciesName);
	}

	@SuppressWarnings("unchecked")
	private List<GKInstance> fetchSpeciesInstances(String speciesName, MySQLAdaptor adaptor) throws Exception {
		// Using a list here because that's what fetchInstanceByAttribute returns but I
		// honestly don't expect more than one result.
		// It would be *very* weird if two different Species objects existed with the
		// same name.
		return new ArrayList<>(adaptor.fetchInstanceByAttribute(
			ReactomeJavaConstants.Species, ReactomeJavaConstants.name, "=", speciesName
		));
	}
}
