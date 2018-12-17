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
	
	
	NonHumanDataProcessor(MySQLAdaptor dbAdaptor, GKInstance instanceEdit)
	{
		this.adaptor = dbAdaptor;
		this.instanceEdit = instanceEdit;
	}
	
	/**
	 * Process data elements whose species is not human.
	 * @param referenceGeneProducts - a reference list of ReferenceGeneProducts
	 * @param data - a Data element. This is an object that is produced by reading the Uniprot XML file.
	 * @param accession - The Uniprot accession.
	 * @throws InvalidAttributeException
	 * @throws InvalidAttributeValueException
	 * @throws Exception
	 */
	void processNonHumanData(Map<String, GKInstance> referenceGeneProducts, UniprotData data, /* List<GKInstance> referenceDNASequencesForThisUniprot, */ String accession) throws InvalidAttributeException, InvalidAttributeValueException, Exception
	{
		if (!referenceGeneProducts.containsKey(accession))
		{
			InstanceCreator creator = new InstanceCreator(adaptor, instanceEdit);
			GKInstance newRefGeneProduct = creator.createNewReferenceGeneProduct(accession);
			adaptor.storeInstance(newRefGeneProduct);
			this.updateInstanceWithData(newRefGeneProduct, data);
			adaptor.updateInstanceAttribute(newRefGeneProduct, ReactomeJavaConstants.referenceGene);
			InstanceDisplayNameGenerator.generateDisplayName(newRefGeneProduct);
			addIsoformsIfNecessary(data, accession, newRefGeneProduct);
			uniprotRecordsLog.info("New UniProt: \"{}\" {} {}", newRefGeneProduct.toString(), accession, newRefGeneProduct.getDBID());
		}
		else
		{
			int instanceCount = 0;
			{
				GKInstance referenceGeneProduct = referenceGeneProducts.get(accession);
				if (!referenceGeneProduct.getSchemClass().isa(ReactomeJavaConstants.ReferenceIsoform))
				{
					if (instanceCount < 1)
					{
						instanceCount++;
						this.updateReferenceGeneProduct(referenceGeneProduct, data, accession);
					}
					else
					{
						referenceDNASequenceLog.info("Duplicate ReferenceGeneProduct instance for identifier {} - this instance will NOT be updated.", accession);
					}
				}
			}
		}
	}
}
