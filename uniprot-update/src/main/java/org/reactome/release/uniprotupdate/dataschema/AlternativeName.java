package org.reactome.release.uniprotupdate.dataschema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="alternativeName")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlternativeName {
	@XmlElement
	private String fullName;
	@XmlElement
	private String shortName;

	public String getFullName() {
		if (fullName == null) {
			return "";
		}

		return "alternativeName: " + fullName;
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
		StringBuilder alternativeNameStringBuilder = new StringBuilder();

		alternativeNameStringBuilder.append(getFullName());

		if (!getShortName().isEmpty()) {
			alternativeNameStringBuilder.append("\t");
			alternativeNameStringBuilder.append(getShortName());
		}

		return alternativeNameStringBuilder.toString();
	}
}
