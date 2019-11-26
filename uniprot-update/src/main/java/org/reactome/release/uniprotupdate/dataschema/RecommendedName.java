package org.reactome.release.uniprotupdate.dataschema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="recommendedName")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecommendedName {
	@XmlElement
	private String fullName;
	@XmlElement
	private String shortName;

	public String getFullName() {
		if (fullName == null) {
			return "";
		}

		return "recommendedName: " + fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getShortName() {
		if (shortName == null) {
			return "";
		}

		return "shortName: " + shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	@Override
	public String toString() {
		return getFullName() + "\t" + getShortName();
	}
}
