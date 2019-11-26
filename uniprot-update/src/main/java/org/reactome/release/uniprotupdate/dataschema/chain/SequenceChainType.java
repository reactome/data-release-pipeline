package org.reactome.release.uniprotupdate.dataschema.chain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Chain types that are allowed in Reactome's import of UniProt data
 * <li>{@link #INITIATOR_METHIONINE}</li>
 * <li>{@link #CHAIN}</li>
 * <li>{@link #PEPTIDE}</li>
 * <li>{@link #PROPEPTIDE}</li>
 * <li>{@link #SIGNAL_PEPTIDE}</li>
 * <li>{@link #TRANSIT_PEPTIDE}</li>
 * <li>{@link #UNKNOWN}</li>
 *
 * @author jweiser
 */
public enum SequenceChainType {
	INITIATOR_METHIONINE ("initiator methionine"),
	CHAIN("chain"),
	PEPTIDE("peptide"),
	PROPEPTIDE("propeptide"),
	SIGNAL_PEPTIDE("signal peptide"),
	TRANSIT_PEPTIDE("transit peptide"),
	UNKNOWN("unknown");

	private static final Logger logger = LogManager.getLogger();

	private String value;

	SequenceChainType(final String value) {
		this.value = value;
	}

	public static SequenceChainType getType(String sequenceChainType) {
		String formattedSequenceChainType = sequenceChainType.toUpperCase().replace(" ", "_");

		try {
			return SequenceChainType.valueOf(formattedSequenceChainType);
		} catch (IllegalArgumentException e) {
			logger.error("{} is an unknown chain type", sequenceChainType, e);
			return SequenceChainType.UNKNOWN;
		}
	}

	@Override
	public String toString() {
		return this.value;
	}
}

