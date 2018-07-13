package org.reactome.release.uniprotupdate.dataschema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="chain")
public class Chain
{
	private String type;
	private String begin;
	private String end;
	private String position;
	
	@XmlElement
	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	@XmlElement
	public String getBegin()
	{
		return begin;
	}

	public void setBegin(String begin)
	{
		this.begin = begin;
	}
	
	@XmlElement
	public String getEnd()
	{
		return end;
	}

	public void setEnd(String end)
	{
		this.end = end;
	}
	
	@XmlElement
	public String getPosition()
	{
		return position;
	}

	public void setPosition(String position) 
	{
		this.position = position;
	}

}
