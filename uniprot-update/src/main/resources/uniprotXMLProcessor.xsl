<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:uniprot="http://uniprot.org/uniprot"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xalan="http://xml.apache.org/xslt">

	<xsl:output method="xml" omit-xml-declaration="yes"
		indent="yes" encoding="utf-8" media-type="text/plain" standalone="yes"
		xalan:indent-amount="4" />

	<xsl:variable name="featureTypes" select="'|chain|peptide|propeptide|signal peptide|transit peptide|'" />

	<xsl:template match="/uniprot:uniprot">
		<simplifiedUniprot>
			<xsl:apply-templates select="./uniprot:entry" />
		</simplifiedUniprot>
	</xsl:template>

	<xsl:template match="uniprot:entry">
		<entry>
			<!-- The "name" element must be the first accession in the list, as per the old Perl code. -->
			<accession>
				<xsl:value-of select="./uniprot:name" />
			</accession>
			<xsl:for-each select="./uniprot:accession">
				<accession>
					<xsl:value-of select="." />
				</accession>
			</xsl:for-each>
			<xsl:apply-templates />
		</entry>
	</xsl:template>

	<xsl:template match="uniprot:organism">
		<xsl:for-each select="./uniprot:name[@type='scientific']">
			<scientificName>
				<xsl:value-of select="." />
			</scientificName>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="uniprot:protein">
		<recommendedName>
			<xsl:text>recommendedName: </xsl:text>
			<xsl:value-of select="./uniprot:recommendedName/uniprot:fullName" />
			<xsl:if test="./uniprot:recommendedName/uniprot:shortName">
				<xsl:text> shortName: </xsl:text>
				<xsl:value-of select="./uniprot:recommendedName/uniprot:shortName" />	
			</xsl:if>
		</recommendedName>
		<xsl:for-each select="./uniprot:alternativeName">
			<alternativeName>
				<xsl:text>alternativeName: </xsl:text>
				<xsl:value-of select="./uniprot:fullName" />
				<xsl:text> shortName: </xsl:text>
				<xsl:value-of select="./uniprot:shortName" />
			</alternativeName>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="uniprot:sequence">
		<sequenceLength>
			<xsl:value-of select="@length" />
		</sequenceLength>
		<sequenceChecksum>
			<xsl:value-of select="@checksum" />
		</sequenceChecksum>
	</xsl:template>

	<xsl:template match="uniprot:gene">
		<gene>
			<xsl:for-each select="./uniprot:name">
				<xsl:element name="name">
					<xsl:attribute name="type">
					<xsl:value-of select="./@type" />
				</xsl:attribute>
					<xsl:value-of select="./text()" />
				</xsl:element>
			</xsl:for-each>
		</gene>
	</xsl:template>

	<xsl:template match="uniprot:dbReference[contains(@type,'Ensembl')]">
		<ensemblGeneID>
			<xsl:value-of select="./uniprot:property[@type='gene ID']/@value" />
		</ensemblGeneID>
	</xsl:template>

	<xsl:template match="uniprot:keyword">
		<xsl:element name="keyword">
			<xsl:attribute name="id">
				<xsl:value-of select="@id" />
			</xsl:attribute>
			<xsl:value-of select="./text()" />
		</xsl:element>
	</xsl:template>

	<xsl:template match="uniprot:comment">
		<!-- this will trigger the templates for "isoform" and "text" elements. -->
		<xsl:apply-templates select="./*" />
	</xsl:template>

	<xsl:template match="uniprot:isoform">
		<xsl:element name="isoform">
			<xsl:attribute name="id">
				<xsl:value-of select="./uniprot:id" />
			</xsl:attribute>
		</xsl:element>
	</xsl:template>

	<xsl:template match="uniprot:text">
		<xsl:element name="commentText">
			<xsl:attribute name="type">
				<xsl:value-of select="../@type" />
			</xsl:attribute>
			<text>
				<xsl:value-of select="." />
			</text>
		</xsl:element>
	</xsl:template>

	<xsl:template match="uniprot:feature[@type='initiator methionine']">
		<xsl:element name="chain">
			<xsl:attribute name="type">
				<xsl:value-of select="@type" />
			</xsl:attribute>
			<xsl:attribute name="position">
				<xsl:value-of
				select="./uniprot:location/uniprot:position/@position" />
			</xsl:attribute>
		</xsl:element>
	</xsl:template>

	<xsl:template match="uniprot:feature">
		<xsl:if test=" contains($featureTypes, concat('|', @type, '|') ) ">
			<xsl:element name="chain">
				<xsl:attribute name="type">
					<xsl:value-of select="@type" />
				</xsl:attribute>
				<xsl:attribute name="begin">
					<xsl:value-of
					select="./uniprot:location/uniprot:begin/@position" />
				</xsl:attribute>
				<xsl:attribute name="end">
					<xsl:value-of
					select="./uniprot:location/uniprot:end/@position" />
				</xsl:attribute>
			</xsl:element>
		</xsl:if>
	</xsl:template>

	<xsl:template match="@* | node()">
		<!-- no-op - ignore everything that gets matched to this template (we're 
			trying to SIMPLIFY the file - remember?) -->
	</xsl:template>
</xsl:stylesheet>