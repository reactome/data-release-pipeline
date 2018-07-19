package org.reactome.release.uniprotupdate.dataschema;

import java.util.List;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="gene")
public class Gene
{
	private List<Name> names;

	@XmlElement(name="name")
	public List<Name> getNames()
	{
		return names;
	}

	public void setNames(List<Name> names)
	{
		this.names = names;
	}
	
}