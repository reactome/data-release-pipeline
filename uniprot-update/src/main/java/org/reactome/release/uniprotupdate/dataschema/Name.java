
package org.reactome.release.uniprotupdate.dataschema;

import java.util.Comparator;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType(name="name")
public class Name
{
	private String type;

	@XmlAttribute(name="type")
	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	private String value;

	@XmlValue
	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public static Comparator<Name> primaryNamesFirst() {
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

	private static boolean isPrimary(Name name) {
		return name.getType().equalsIgnoreCase("primary");
	}
}