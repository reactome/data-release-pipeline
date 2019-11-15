package org.reactome.release.uniprotupdate.dataschema;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Class representing an Isoform of a UniProt entry
 * @author sshorser
 */
@XmlType(name="isoform")
public class Isoform {
	private String isoformID;

	/**
	 * Returns the identifier of the Isoform
	 * @return The identifier Isoform or an empty String if none
	 */
	@XmlAttribute(name="id")
	public String getIsoformID() {
		if (isoformID == null) {
			return "";
		}

		return isoformID;
	}

	/**
	 * Sets the identifier of the Isoform
	 * @param isoformID The identifier Isoform
	 */
	public void setIsoformID(String isoformID) {
		this.isoformID = isoformID;
	}

}