package org.reactome.release.uniprotupdate;

import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;

public class NonHumanDataProcessor extends AbstractDataProcessor
{

	public NonHumanDataProcessor(MySQLAdaptor adaptor, GKInstance instanceEdit)
	{
		super(adaptor, instanceEdit);
	}

	/**
	 * Process data elements whose species is not human.
	 * @param referenceGeneProducts - a reference list of ReferenceGeneProducts
	 * @param data - a Data element. This is an object that is produced by reading the Uniprot XML file.
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 * @throws Exception
	 */
	void processNonHumanData(Map<String, GKInstance> referenceGeneProducts, UniprotData data)
		throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		String primaryAccession = data.getPrimaryAccession();
		if (!referenceGeneProducts.containsKey(primaryAccession))
		{
			InstanceCreator creator = new InstanceCreator(adaptor, instanceEdit);
			GKInstance newRefGeneProduct = creator.createNewReferenceGeneProduct(primaryAccession);
			adaptor.storeInstance(newRefGeneProduct);
			this.updateInstanceWithData(newRefGeneProduct, data);
			adaptor.updateInstanceAttribute(newRefGeneProduct, ReactomeJavaConstants.referenceGene);
			InstanceDisplayNameGenerator.generateDisplayName(newRefGeneProduct);
			addIsoformsIfNecessary(data, primaryAccession, newRefGeneProduct);
			uniprotRecordsLog.info(
				"New UniProt: \"{}\" {} {}",
				newRefGeneProduct.toString(), primaryAccession, newRefGeneProduct.getDBID()
			);
		}
		else
		{
			GKInstance referenceGeneProduct = referenceGeneProducts.get(primaryAccession);
			if (!referenceGeneProduct.getSchemClass().isa(ReactomeJavaConstants.ReferenceIsoform))
			{
				this.updateReferenceGeneProduct(referenceGeneProduct, data, primaryAccession);
			}
		}
	}
}
