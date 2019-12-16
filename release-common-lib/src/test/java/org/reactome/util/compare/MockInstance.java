package org.reactome.util.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class MockInstance {
	@Mock
	GKInstance instance;
	@Mock
	SchemaClass schemaClass;
	@Mock
	MySQLAdaptor adaptor;

	Collection<SchemaAttribute> attributes;
	Collection<SchemaAttribute> reverseAttributes;

	public static MockInstance createMockInstance(String instanceClassName) {
		return new MockInstance(instanceClassName);
	}

	private MockInstance(String instanceClassName) {
		attributes = new ArrayList<>();
		reverseAttributes = new ArrayList<>();

		MockitoAnnotations.initMocks(this);

		Mockito.when(instance.getSchemClass()).thenReturn(schemaClass);
		Mockito.when(instance.getDbAdaptor()).thenReturn(adaptor);
		Mockito.when(adaptor.getDBName()).thenReturn("mock database");
		Mockito.when(schemaClass.getName()).thenReturn(instanceClassName);
		Mockito.when(schemaClass.getAttributes()).thenReturn(attributes);
		Mockito.when(schemaClass.getReferers()).thenReturn(reverseAttributes);
	}

	public void addMockAttribute(
		String attributeName, Class<?> attributeType, List<?> attributeValues
	) throws Exception {
		Mockito.when(instance.getAttributeValuesList(attributeName)).thenReturn(attributeValues);

		attributes.add(MockAttribute.createMockAttribute(attributeName, attributeType));
	}

	public void addMockAttribute(SchemaAttribute attribute) {
		attributes.add(attribute);
	}

	public void addMockReverseAttribute(
		String attributeName, Class<?> attributeType, List<?> attributeValues
	) throws Exception {
		Mockito.when(instance.getReferers(attributeName)).thenReturn(attributeValues);

		reverseAttributes.add(MockAttribute.createMockAttribute(attributeName, attributeType));
	}

	public void addMockReverseAttribute(SchemaAttribute reverseAttribute) {
		reverseAttributes.add(reverseAttribute);
	}

	public GKInstance getGKInstance() {
		return this.instance;
	}
}
