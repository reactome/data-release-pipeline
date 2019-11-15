package org.reactome.release.uniprotupdate.dataschema.chain;

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
	INITIATOR_METHIONINE,
	CHAIN,
	PEPTIDE,
	PROPEPTIDE,
	SIGNAL_PEPTIDE,
	TRANSIT_PEPTIDE,
	UNKNOWN
}