package org.reactome.release.uniprotupdate.dataschema;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="isoform")
public class Isoform
{
	private String isoformID;

	@XmlAttribute(name="id")
	public String getIsoformID()
	{
		return isoformID;
	}

	public void setIsoformID(String isoformID)
	{
		this.isoformID = isoformID;
	}
	
}