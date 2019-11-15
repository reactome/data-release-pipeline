package org.reactome.release.uniprotupdate.dataschema.chain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Abstract class to represent a sequence chain in a UniProt entry
 * @author jweiser
 */
@XmlType(name="chain")
public abstract class SequenceChain {
	private SequenceChainType type;

	/**
	 * Returns the type of the chain of a UniProt entry
	 * @return Type of the chain or an ChainType.UNKNOWN if there is none
	 */
	@XmlAttribute(name="type")
	public SequenceChainType getType() {
		if (type == null) {
			return SequenceChainType.UNKNOWN;
		}

		return type;
	}

	/**
	 * Sets the SequenceChainType to assign to this chain of a UniProt entry
	 * @param type SequenceChainType to assign to this chain of a UniProt entry
	 */
	public void setType(SequenceChainType type) {
		this.type = type;
	}
}
