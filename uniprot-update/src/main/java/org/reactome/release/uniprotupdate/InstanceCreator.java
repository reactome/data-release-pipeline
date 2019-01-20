package org.reactome.release.uniprotupdate;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;

import java.util.List;

/**
 * This class creates Instances.
 * @author sshorser
 *
 */
public class InstanceCreator
{
	private MySQLAdaptor adaptor;
	private GKInstance creationInstanceEdit;

	/**
	 * Constructs a new InstanceCreator.
	 * @param dbAdaptor - the datbase adaptor that will be used by this object to create instances.
	 * @param instance - the InstanceEdit that will be associated with the created object.
	 */
	public InstanceCreator(MySQLAdaptor dbAdaptor, GKInstance instance)
	{
		this.adaptor = dbAdaptor;
		this.creationInstanceEdit = instance;
	}

	/**
	 * Creates a new ReferenceGeneProduct. NOTE: this method does not *persist* the new instance.
	 * @param accession - the Uniprot Accession for the new RGP.
	 * @return A newly created ReferenceGeneProduct, whose identifier is <code>accession</code>
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 */
	GKInstance createNewReferenceGeneProduct(String accession)
		throws InvalidAttributeException, InvalidAttributeValueException
	{
		GKInstance referenceGeneProduct = new GKInstance(
			this.adaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceGeneProduct)
		);
		referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.created, this.creationInstanceEdit);
		referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.modified, this.creationInstanceEdit);
		referenceGeneProduct.setAttributeValue(
			ReactomeJavaConstants.referenceDatabase,
			InstanceFetcher.getInstanceFetcher(this.adaptor).getUniprotRefDB()
		);
		referenceGeneProduct.setAttributeValue(ReactomeJavaConstants.identifier, accession);
		referenceGeneProduct.setDbAdaptor(this.adaptor);
		return referenceGeneProduct;
	}

	/**
	 * Creates a new ReferenceDNASequence. NOTE: this method does not *persist* the new instance.
	 * @param geneNames - Gene names for this ReferencDNASequence.
	 * @param primaryGeneName - the Primary gene name.
	 * @param ensemblGeneID - the ENSEMBL Gene ID.
	 * @return a newly created ReferenceDNASequence.
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 */
	GKInstance createNewReferenceDNASequence(List<String> geneNames, String primaryGeneName, String ensemblGeneID)
		throws InvalidAttributeException, InvalidAttributeValueException
	{
		GKInstance newRefDNASequence = new GKInstance(
			this.adaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDNASequence)
		);
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.identifier, ensemblGeneID);
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.species,
				InstanceFetcher.getInstanceFetcher(this.adaptor).getHumanSpecies());
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.referenceDatabase,
				InstanceFetcher.getInstanceFetcher(this.adaptor).getEnsemblHSapiensRefDB());
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.geneName, geneNames);
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.created, this.creationInstanceEdit);
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.modified, null);
		newRefDNASequence.setAttributeValue(ReactomeJavaConstants.name, primaryGeneName);
		newRefDNASequence.setDbAdaptor(this.adaptor);
		return newRefDNASequence;
	}

	/**
	 * Creates a new ReferenceIsoform AND persists it in the database.
	 * @param accession - the Uniprot accession.
	 * @param referenceGeneProduct - The ReferenceGeneProduct that will b used for the "isoformParent" attribute.
	 * @param isoformID - the Isoform ID.
	 * @return the DBID of the new Isoform.
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 * @throws Exception
	 */
	long createNewReferenceIsoform(String accession, GKInstance referenceGeneProduct, String isoformID)
		throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		long dbID;
		GKInstance referenceIsoform = new GKInstance(this.adaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceIsoform));
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.created, this.creationInstanceEdit);
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.identifier, accession);
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.referenceDatabase,
				InstanceFetcher.getInstanceFetcher(this.adaptor).getUniprotRefDB());
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.isoformParent, referenceGeneProduct);
		referenceIsoform.setAttributeValue(ReactomeJavaConstants.variantIdentifier, isoformID);
		referenceIsoform.setDbAdaptor(this.adaptor);
		// now update with the rest of the values in "data"...
		dbID = adaptor.storeInstance(referenceIsoform);
		InstanceDisplayNameGenerator.setDisplayName(referenceIsoform);
		this.adaptor.updateInstanceAttribute(referenceIsoform, ReactomeJavaConstants._displayName);
		return dbID;
	}
}
