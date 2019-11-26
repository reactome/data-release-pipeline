package org.reactome.release.uniprotupdate.dataschema.chain;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Abstract class to represent a sequence chain in a UniProt entry
 * @author jweiser
 */
public abstract class SequenceChain {
	@XmlAttribute(name="type")
	private String type;

	/**
	 * Returns the type of the chain of a UniProt entry
	 * @return Type of the chain or an ChainType.UNKNOWN if there is none
	 */
	public SequenceChainType getType() {
		return SequenceChainType.getType(type);
	}

	/**
	 * Sets the SequenceChainType to assign to this chain of a UniProt entry
	 * @param type SequenceChainType to assign to this chain of a UniProt entry
	 */
	void setType(String type) {
		this.type = type;
	}
}
