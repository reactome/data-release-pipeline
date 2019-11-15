package org.reactome.release.uniprotupdate.dataschema;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Class representing a Keyword of a UniProt entry
 * @author sshorser
 */
@XmlType(name="keyword")
public class Keyword {
	private String id;
	private String keyword;

	/**
	 * Returns the id of the Keyword
	 * @return The id of the Keyword or an empty String if none
	 */
	@XmlAttribute(name="id")
	public String getId() {
		if (id == null) {
			return "";
		}

		return id;
	}

	/**
	 * Sets the id of the Keyword
	 * @param id The id of the Keyword
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the Keyword text value
	 * @return The Keyword text value or an empty String if none
	 */
	@XmlValue
	public String getKeyword() {
		if (keyword == null) {
			return "";
		}

		return keyword;
	}

	/**
	 * Sets the text value of the Keyword
	 * @param value The text value of the Keyword
	 */
	public void setKeyword(String value) {
		this.keyword = value;
	}
}