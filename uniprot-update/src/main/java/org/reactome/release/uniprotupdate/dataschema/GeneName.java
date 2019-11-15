package org.reactome.release.uniprotupdate.dataschema;

import java.util.Comparator;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Class representing a GeneName of a UniProt entry
 * @author sshorser, jweiser
 */
@XmlType(name="name")
public class GeneName {
	private String type;
	private String value;

	/**
	 * Returns the type of the GeneName
	 * @return The type of the GeneName or an empty String if none
	 */
	@XmlAttribute(name="type")
	public String getType() {
		if (type == null) {
			return "";
		}

		return type;
	}

	/**
	 * Sets the type of the GeneName
	 * @param type The type of the GeneName
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the value of the GeneName
	 * @return The value of the GeneName or an empty String if none
	 */
	@XmlValue
	public String getValue() {
		if (value == null) {
			return "";
		}

		return value;
	}

	/**
	 * Sets the value of the GeneName
	 * @param value The value of the GeneName
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Comparator to sort GeneNames with primary names being listed first.  If neither or both names are primary, the
	 * names remain in the order in which they were received.
	 * @return Comparator which compares GeneNames and gives primary gene names priority
	 */
	public static Comparator<GeneName> primaryNamesFirst() {
		return (name1, name2) -> {
			if (isPrimary(name1) && !isPrimary(name2)) {
				return -1; // Name 1 is first
			} else if (!isPrimary(name1) && isPrimary(name2)) {
				return 1; // Name 2 is first
			} else {
				return 0; // Equivalent if both or neither of the names are primary
			}
		};
	}

	/**
	 * Returns true if the geneName object passed represents a primary gene name and false otherwise.
	 * @param geneName GeneName object to determine if it is a primary gene name
	 * @return true if a primary gene name, false otherwise
	 * @throws IllegalArgumentException Thrown if the geneName object passed is null
	 */
	private static boolean isPrimary(GeneName geneName) {
		if (geneName == null) {
			throw new IllegalArgumentException("The geneName object passed is null");
		}

		return geneName.getType().equalsIgnoreCase("primary");
	}
}