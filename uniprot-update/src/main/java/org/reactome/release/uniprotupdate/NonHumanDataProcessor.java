package org.reactome.release.uniprotupdate;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

class NonHumanDataProcessor extends AbstractDataProcessor {

	NonHumanDataProcessor(MySQLAdaptor adaptor, GKInstance instanceEdit) {
		super(adaptor, instanceEdit);
	}

	/**
	 * Process data elements whose species is not human.
	 * @param uniprotEntry - a Data element. This is an object that is produced by reading the Uniprot XML file.
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 * @throws Exception
	 */
	@Override
	void processData(UniprotData uniprotEntry) throws InvalidAttributeException, InvalidAttributeValueException, Exception {
		String primaryAccession = uniprotEntry.getPrimaryAccession();
		if (accessionInDatabase(primaryAccession, adaptor)) {
			if (!isAnIsoform(primaryAccession)) {
				this.updateReferenceGeneProduct(getReferenceGeneProduct(primaryAccession), uniprotEntry);
			}
		} else {
			GKInstance newRefGeneProduct = createReferenceGeneProduct(adaptor, instanceEdit, uniprotEntry);

			IsoformProcessor isoformProcessor = new IsoformProcessor();
			isoformProcessor.updateOrCreateIsoforms(newRefGeneProduct, uniprotEntry);
			uniprotRecordsLog.info(
				"New UniProt: \"{}\" {} {}",
				newRefGeneProduct.toString(), primaryAccession, newRefGeneProduct.getDBID()
			);
		}
	}

	private boolean accessionInDatabase(String accession, MySQLAdaptor adaptor) throws Exception {
		return !getReferenceGeneProducts(adaptor).containsKey(accession);
	}

	private boolean isAnIsoform(String accession) throws Exception {
		return getReferenceGeneProduct(accession).getSchemClass().isa(ReactomeJavaConstants.ReferenceIsoform);
	}

	private GKInstance getReferenceGeneProduct(String accession) throws Exception {
		return getReferenceGeneProducts(adaptor).get(accession);
	}

	private GKInstance createReferenceGeneProduct(
		MySQLAdaptor adaptor, GKInstance instanceEdit, UniprotData uniprotEntry
	) throws Exception {
		GKInstance newRefGeneProduct = new InstanceCreator(adaptor, instanceEdit)
			.createNewReferenceGeneProduct(uniprotEntry.getPrimaryAccession());
		adaptor.storeInstance(newRefGeneProduct);

		this.updateInstanceWithData(newRefGeneProduct, uniprotEntry);
		adaptor.updateInstanceAttribute(newRefGeneProduct, ReactomeJavaConstants.referenceGene);
		InstanceDisplayNameGenerator.generateDisplayName(newRefGeneProduct);

		return newRefGeneProduct;
	}
}
