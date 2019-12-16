package org.reactome.util.compare;

import static org.gk.model.ReactomeJavaConstants.accession;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.gk.model.ReactomeJavaConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.gk.model.GKInstance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

public class DBObjectComparerTest {
	private StringBuilder stringBuilder;

	private final String DUMMY_ACCESSION_1 = "0000001";
	private final String DUMMY_ACCESSION_2 = "0000002";
	private final String DUMMY_INSTANCE_OF_ACCESSION_1 = "0000003";
	private final String DUMMY_INSTANCE_OF_ACCESSION_2 = "0000004";
	private final List<String> DUMMY_INSTANCE_OF_ACCESSION_LIST_1 =
		Collections.singletonList(DUMMY_INSTANCE_OF_ACCESSION_1);
	private final List<String> DUMMY_INSTANCE_OF_ACCESSION_LIST_2 =
		Collections.singletonList(DUMMY_INSTANCE_OF_ACCESSION_2);

	@BeforeEach
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
		stringBuilder = new StringBuilder();
	}

	@Test
	public void differenceCountIsZeroWhenInstancesAreNull() {
		int differenceCount = DBObjectComparer.compareInstances(null, null, stringBuilder);

		assertThat(differenceCount, is(equalTo(0)));
	}

	@Test
	public void differenceCountIsZeroWhenInstanceIsAnInstanceEdit() {
		MockInstance instance1 = MockInstance.createMockInstance(InstanceEdit);
		MockInstance instance2 = MockInstance.createMockInstance(GO_BiologicalProcess);

		int differenceCount = DBObjectComparer.compareInstances(
			instance1.getGKInstance(), instance2.getGKInstance(), stringBuilder
		);

		assertThat(differenceCount, is(equalTo(0)));
	}

	@Test
	public void differenceCountIsOneWhenInstancesAreDifferentTypes() {
		MockInstance instance1 = MockInstance.createMockInstance(GO_MolecularFunction);
		MockInstance instance2 = MockInstance.createMockInstance(GO_BiologicalProcess);

		int differenceCount = DBObjectComparer.compareInstances(
			instance1.getGKInstance(), instance2.getGKInstance(), stringBuilder);

		assertThat(differenceCount, is(equalTo(1)));
	}

	@Test
	public void differenceCountIsOneWhenInstancesHaveDifferentNumberOfValuesForAnAttribute() throws Exception {
		final List<String> oneInstanceOfValue = DUMMY_INSTANCE_OF_ACCESSION_LIST_1;
		final List<String> twoInstanceOfValues = Arrays.asList(
			DUMMY_INSTANCE_OF_ACCESSION_1, DUMMY_INSTANCE_OF_ACCESSION_2
		);

		GKInstance instance1 = createMockGOBiologicalProcess(DUMMY_ACCESSION_1, oneInstanceOfValue);
		GKInstance instance2 = createMockGOBiologicalProcess(DUMMY_ACCESSION_1, twoInstanceOfValues);

		int differenceCount = DBObjectComparer.compareInstances(instance1, instance2, stringBuilder);

		assertThat(differenceCount, is(equalTo(1)));
	}

	@Test
	public void differenceCountIsZeroWhenInstancesHaveTheSameAttributeValues() throws Exception {
		GKInstance instance1 = createMockGOBiologicalProcess(DUMMY_ACCESSION_1);
		GKInstance instance2 = createMockGOBiologicalProcess(DUMMY_ACCESSION_1);

		int differenceCount = DBObjectComparer.compareInstances(instance1, instance2, stringBuilder);

		assertThat(differenceCount, is(equalTo(0)));
	}

	@Test
	public void differenceCountIsOneWhenInstancesHaveOneDifferentAttributeValue() throws Exception {
		GKInstance instance1 = createMockGOBiologicalProcess(DUMMY_ACCESSION_1);
		GKInstance instance2 = createMockGOBiologicalProcess(DUMMY_ACCESSION_2);

		int differenceCount = DBObjectComparer.compareInstances(instance1, instance2, stringBuilder);

		assertThat(differenceCount, is(equalTo(1)));
	}

	@Test
	public void differenceCountIsTwoWhenInstancesHaveTwoDifferentAttributeValues() throws Exception {
		GKInstance instance1 = createMockGOBiologicalProcess(
			DUMMY_ACCESSION_1, DUMMY_INSTANCE_OF_ACCESSION_LIST_1
		);
		GKInstance instance2 = createMockGOBiologicalProcess(
			DUMMY_ACCESSION_2, DUMMY_INSTANCE_OF_ACCESSION_LIST_2
		);

		int differenceCount = DBObjectComparer.compareInstances(instance1, instance2, stringBuilder);

		assertThat(differenceCount, is(equalTo(2)));
	}

	@Test
	public void differenceCountIsTwoWhenInstancesHaveTwoDifferentAttributeValuesIncludingReferrers() throws Exception {
		GKInstance instance1 = createMockGOBiologicalProcessWithInstanceOfReferrers(
			DUMMY_ACCESSION_1, DUMMY_INSTANCE_OF_ACCESSION_LIST_1
		);
		GKInstance instance2 = createMockGOBiologicalProcessWithInstanceOfReferrers(
			DUMMY_ACCESSION_2, DUMMY_INSTANCE_OF_ACCESSION_LIST_2
		);

		final boolean checkReferrers = true;
		int differenceCount = DBObjectComparer.compareInstances(instance1, instance2, stringBuilder, checkReferrers);

		assertThat(differenceCount, is(equalTo(2)));
	}

	@Test
	public void differenceCountIsOneWhenInstancesHaveOneDifferentAttributeValueExcludingReferrers() throws Exception {
		GKInstance instance1 = createMockGOBiologicalProcessWithInstanceOfReferrers(
			DUMMY_ACCESSION_1, DUMMY_INSTANCE_OF_ACCESSION_LIST_1
		);
		GKInstance instance2 = createMockGOBiologicalProcessWithInstanceOfReferrers(
			DUMMY_ACCESSION_2, DUMMY_INSTANCE_OF_ACCESSION_LIST_2
		);

		int differenceCount = DBObjectComparer.compareInstances(instance1, instance2, stringBuilder);

		assertThat(differenceCount, is(equalTo(1)));
	}

	private GKInstance createMockGOBiologicalProcess(String accessionValue) throws Exception {
		return createMockGOBiologicalProcess(accessionValue, Collections.emptyList());
	}

	private GKInstance createMockGOBiologicalProcess(String accessionValue, List<String> instanceOfAccessionValues)
		throws Exception {
		return createMockGOBiologicalProcessWithInstanceOfValues(accessionValue, instanceOfAccessionValues, false);
	}

	private GKInstance createMockGOBiologicalProcessWithInstanceOfReferrers(
		String accessionValue, List<String> instanceOfAccessionValues
	) throws Exception {
		return createMockGOBiologicalProcessWithInstanceOfValues(accessionValue, instanceOfAccessionValues, true);
	}

	private GKInstance createMockGOBiologicalProcessWithInstanceOfValues(
		String accessionValue, List<String> instanceOfAccessionValues, boolean asReferrers
	) throws Exception {
		final String className = GO_BiologicalProcess;

		MockInstance mockInstance = MockInstance.createMockInstance(className);
		mockInstance.addMockAttribute(accession, String.class, Collections.singletonList(accessionValue));

		if (asReferrers) {
			mockInstance.addMockReverseAttribute(instanceOf, GKInstance.class,
				createInstanceOfAttributeValues(className, instanceOfAccessionValues)
			);
		} else {
			mockInstance.addMockAttribute(instanceOf, GKInstance.class,
				createInstanceOfAttributeValues(className, instanceOfAccessionValues)
			);
		}

		return mockInstance.getGKInstance();
	}

	private List<GKInstance> createInstanceOfAttributeValues(String className, List<String> instanceOfAccessionValues)
		throws Exception {

		List<GKInstance> instancesOf = new ArrayList<>();
		for (String instanceOfAccessionValue : instanceOfAccessionValues) {
			MockInstance instanceOf = MockInstance.createMockInstance(className);
			instanceOf.addMockAttribute(accession, String.class, Collections.singletonList(instanceOfAccessionValue));
			instancesOf.add(instanceOf.getGKInstance());
		}

		return instancesOf;
	}
}
