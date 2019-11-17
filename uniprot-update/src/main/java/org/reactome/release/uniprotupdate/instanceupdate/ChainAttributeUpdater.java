package org.reactome.release.uniprotupdate.instanceupdate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.reactome.release.uniprotupdate.dataschema.Chain;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ChainAttributeUpdater extends AttributeUpdater {
	private static final Logger sequencesLog = LogManager.getLogger("sequencesLog");
	private static final String CHAIN_CHANGE_LOG = "_chainChangeLog";

	ChainAttributeUpdater(String attributeToUpdate) {
		super(attributeToUpdate);
	}

	@Override
	protected List<Chain> extractAttributeDataFromUniprot(UniprotData data) {
		return data.getChains() != null ? data.getChains() : new ArrayList<>();
	}

	/**
	 * Updates the "chain" attribute on an instance.
	 * @param instance - the instance to update.
	 * @param data - the UniprotData object on which to base the chain update.
	 * @throws Exception
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 */
	private void updateChain(GKInstance instance, UniprotData data)
		throws Exception, InvalidAttributeException, InvalidAttributeValueException {
		List<String> newChains = getNewChains(data);

		logChainChanges(new HashSet<>(newChains), instance);
		instance.setAttributeValue("chain", newChains);
		instance.getDbAdaptor().updateInstanceAttribute(instance, "chain");
	}

	private List<String> getNewChains(UniprotData data) {
		return data.getChains()
			.stream()
			.map(Chain::toString)
			.collect(Collectors.toList());
	}

	/**
	 * Updates the "_chainChangeLog" attribute for an Instance in the database.
	 * @param newChains - A set of new chains to log.
	 * @param instance - The Instance that the chains are associated with.
	 * @throws Exception
	 */
	private void logChainChanges(Set<String> newChains, GKInstance instance) throws Exception {
		String additionalChainLogEntries = getAdditionalChainLogEntries(instance, newChains);

		if (!additionalChainLogEntries.isEmpty()) {
			sequencesLog.info("Chain differences: {} ; For: {}", additionalChainLogEntries, instance.toString());
			instance.setAttributeValue(
				CHAIN_CHANGE_LOG, getUpdatedChainLog(getCurrentChainLog(instance), additionalChainLogEntries)
			);
			instance.getDbAdaptor().updateInstanceAttribute(instance, CHAIN_CHANGE_LOG);
		}
	}

	private String getCurrentChainLog(GKInstance referenceGeneProduct) throws Exception {
		String changeLog = (String) referenceGeneProduct.getAttributeValue(CHAIN_CHANGE_LOG);

		return changeLog != null ? changeLog.trim() : "";
	}

	private String getAdditionalChainLogEntries(GKInstance referenceGeneProduct, Set<String> newChains)
		throws Exception {
		List<String> additionalChainLogEntries = new ArrayList<>();

		@SuppressWarnings("unchecked")
		Set<String> oldChains = new HashSet<>(referenceGeneProduct.getAttributeValuesList("chain"));

		List<String> removedChains = oldChains.stream().filter(oldChain -> !newChains.contains(oldChain)).collect(Collectors.toList());
		List<String> removedChainLogEntries = getChainLogEntries(referenceGeneProduct.getDBID(), removedChains, "removed");
		additionalChainLogEntries.addAll(removedChainLogEntries);

		List<String> addedChains = newChains.stream().filter(newChain -> !oldChains.contains(newChain)).collect(Collectors.toList());
		List<String> addedChainLogEntries = getChainLogEntries(referenceGeneProduct.getDBID(), addedChains, "added");
		additionalChainLogEntries.addAll(addedChainLogEntries);

		return String.join(";", additionalChainLogEntries);
	}

	private List<String> getChainLogEntries(long instanceDBID, List<String> chains, String actionPerformed) {
		return chains
			.stream()
			.map(chain ->
				chain + " for " + instanceDBID + " " + actionPerformed + " on " + getCurrentDateString()
			)
			.collect(Collectors.toList());
	}

	private String getCurrentDateString() {
		// Perl code was:
		// my $t = localtime;
		// my $date = $t->day . ' ' . $t->fullmonth . ' ' . $t->mday . ' ' . $t->year;
		// See "Patterns for Formatting and Parsing" on this page:
		// https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
		LocalDateTime currentDate = LocalDateTime.now();
		return DateTimeFormatter.ofPattern("EEEE LLLL dd yyyy").toFormat().format(currentDate);
	}

	private String getUpdatedChainLog(String currentChainLog, String additionalChainLogEntries) {
		return currentChainLog.isEmpty() ?
			additionalChainLogEntries :
			currentChainLog.concat(";").concat(additionalChainLogEntries);
	}
}