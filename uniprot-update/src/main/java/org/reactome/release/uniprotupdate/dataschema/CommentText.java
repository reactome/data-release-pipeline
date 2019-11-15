package org.reactome.release.uniprotupdate.dataschema;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Class representing a free-text comment in a UniProt entry
 * @author sshorser
 */
@XmlType(name="commentText")
public class CommentText {
	private String type;
	private String text;

	/**
	 * Returns the type of the comment as categorized by UniProt
	 * @return Type of the comment or an empty String if none
	 */
	@XmlAttribute(name="type")
	public String getType() {
		if (type == null) {
			return "";
		}

		return type;
	}

	/**
	 * Sets the type of the comment as categorized by UniProt
	 * @param type Type of the comment
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the text of the comment
	 * @return Text of the comment of an empty String if none
	 */
	@XmlElement(name="text")
	public String getText() {
		if (text == null) {
			return "";
		}

		return text;
	}

	/**
	 * Sets the type of the comment
	 * @param text Text of the comment
	 */
	public void setText(String text) {
		this.text = text;
	}


	/**
	 * String representation of the comment as "type text" (e.g. "").
	 * @return String detailing the type and text of this comment
	 */
	@Override
	public String toString() {
		return getType() + " " + getText();
	}

}