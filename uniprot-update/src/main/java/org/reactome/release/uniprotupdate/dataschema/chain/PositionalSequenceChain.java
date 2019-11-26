package org.reactome.release.uniprotupdate.dataschema.chain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Class to represent a sequence chain of length 1 at a specific position in a UniProt entry
 * @author jweiser
 */
@XmlType(name="positionalChain")
public class PositionalSequenceChain extends SequenceChain {
	private int position;

	/**
	 * Returns the position of the chain of a UniProt entry
	 * @return Position of the chain
	 */
	@XmlAttribute(name="position")
	public int getPosition() {
		return position;
	}

	/**
	 * Sets the position of this chain of a UniProt entry
	 * @param position Position to assign to this chain of a UniProt entry (value must be greater than or equal to 1)
	 * @throws IllegalArgumentException Thrown if the position passed is less than 1
	 */
	public void setPosition(int position) {
		final int MINIMUM_POSITION = 1;

		if (position < MINIMUM_POSITION) {
			throw new IllegalArgumentException(
				"Position passed was " + position + ".  It must be an integer value no less than " +
					MINIMUM_POSITION
			);
		}

		this.position = position;
	}

	/**
	 * String representation of the positional sequence chain as "type:position" (e.g. "initiator methionine:1").
	 * @return String detailing the type and position of this sequence chain
	 */
	@Override
	public String toString() {
		return super.getType().toString() + ":" + getPosition();
	}
}
