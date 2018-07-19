package org.reactome.release.uniprotupdate.dataschema;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType(name="keyword")
public class Keyword
{
	private String id;
	
	private String keyword;

	@XmlAttribute(name="id")
	public String getId()
	{
		return id;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}

	@XmlValue
	public String getKeyword()
	{
		return keyword;
	}
	
	public void setKeyword(String value)
	{
		this.keyword = value;
	}
}