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

	/**
	 * Returns the string-form of this chain, for Reactome.
	 * The posibilities are: "initiator methionine:&lt;position&gt;" if there is a value for position
	 * or "&lt;type&gt;:&lt;begin&gt;-&lt;end&gt;" if there are values for type, begin, end. Otherwise, super.toString() is called.
	 */
	@Override
	public String toString()
	{
		String chainAsString = "";
		// if the string should be "initiator methionine:<position>"
		if (position!=null)
		{
			chainAsString = "initiator methionine:" + this.position;
		}
		// if there are begin/end values, the string should be "<type>:<begin>-<end>"
		else if (begin != null && end != null && type != null)
		{
			chainAsString = type+":"+begin+"-"+end;
		}
		
		if (chainAsString.equals(""))
		{
			return super.toString();
		}
		else
		{
			return chainAsString;
		}
	}
	
}
