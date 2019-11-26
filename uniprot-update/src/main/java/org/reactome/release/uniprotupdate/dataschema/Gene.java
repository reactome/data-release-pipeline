package org.reactome.release.uniprotupdate.dataschema;

import java.util.ArrayList;
import java.util.List;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Class representing a Gene which encodes a UniProt entry
 * @author sshorser
 */
@XmlType(name="gene")
@XmlAccessorType(XmlAccessType.FIELD)
public class Gene {
	@XmlElement(name="name")
	private List<GeneName> names;

	/**
	 * Returns the list of names associated with this Gene object
	 * @return List of GeneName objects or an empty list if there are none
	 */
	public List<GeneName> getNames() {
		if (names == null) {
			return new ArrayList<>();
		}

		return names;
	}

	/**
	 * Sets the list of names associated with this Gene object
	 * @param names List of GeneName objects
	 */
	public void setNames(List<GeneName> names) {
		this.names = names;
	}

}