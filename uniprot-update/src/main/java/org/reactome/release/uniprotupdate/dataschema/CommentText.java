package org.reactome.release.uniprotupdate.dataschema;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="commentText")
public class CommentText
{
	private String type;
	private String text;
	
	@XmlAttribute(name="type")
	public String getType()
	{
		return type;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	
	@XmlElement(name="text")
	public String getText()
	{
		return text;
	}
	public void setText(String text)
	{
		this.text = text;
	}
}