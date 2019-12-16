package org.reactome.util.compare;

import org.gk.schema.SchemaAttribute;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class MockAttribute {
	@Mock
	SchemaAttribute attribute;

	public static SchemaAttribute createMockAttribute(String attributeName, Class<?> attributeType) {
		return new MockAttribute(attributeName, attributeType).getAttribute();
	}

	private MockAttribute(String attributeName, Class<?> attributeType) {
		MockitoAnnotations.initMocks(this);

		Mockito.when(attribute.getType()).thenReturn(attributeType);
		Mockito.when(attribute.getName()).thenReturn(attributeName);
	}

	private SchemaAttribute getAttribute() {
		return this.attribute;
	}
}
