package org.reactome.release.uniprotupdate.dataschema.chain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Class to represent a sequence chain with a specific range (i.e. a beginning and ending position) in a UniProt entry
 * @author jweiser
 */
@XmlType(name="rangedChain")
public class RangedSequenceChain extends SequenceChain {
	private int begin;
	private int end;

	/**
	 * Returns the beginning position of the chain of a UniProt entry
	 * @return Beginning position of the chain
	 */
	@XmlAttribute(name="begin")
	public int getBegin() {
		return begin;
	}

	/**
	 * Sets the beginning position to this chain of a UniProt entry
	 * @param begin Beginning position to assign to this chain of a UniProt entry
	 * (value must be greater than or equal to 1)
	 * @throws IllegalArgumentException Thrown if the beginning position passed is less than 1
	 */
	public void setBegin(int begin) throws IllegalArgumentException {
		final int MINIMUM_BEGIN_POSITION = 1;

		if (begin < MINIMUM_BEGIN_POSITION) {
			throw new IllegalArgumentException(
				"Begin position passed was " + begin + ".  It must be an integer value no less than " +
					MINIMUM_BEGIN_POSITION
			);
		}

		this.begin = begin;
	}

	/**
	 * Returns the ending position of the chain of a UniProt entry
	 * @return Ending position of the chain
	 */
	@XmlAttribute(name="end")
	public int getEnd() {
		return end;
	}

	/**
	 * Sets the ending position to this chain of a UniProt entry
	 * @param end Ending position to assign to this chain of a UniProt entry
	 * (value must be less than or equal to the current beginning position)
	 * @see #getBegin()
	 * @throws IllegalArgumentException Thrown if the ending position passed is greater than the current beginning
	 * position obtained from #getBegin()
	 */
	public void setEnd(int end) {
		if (end < getBegin()) {
			throw new IllegalArgumentException(
				"End position passed was " + end + ".  It must be an integer value greater than or equal to the " +
					"begin position of " + getBegin()
			);
		}

		this.end = end;
	}

	/**
	 * String representation of the ranged sequence chain as "type:begin-end" (e.g. "chain:1-25").
	 * @return String detailing the type and range (i.e. begin and end positions) of this sequence chain
	 */
	@Override
	public String toString() {
		return super.getType().toString() + ":" + getBegin() + "-" + getEnd();
	}
}
