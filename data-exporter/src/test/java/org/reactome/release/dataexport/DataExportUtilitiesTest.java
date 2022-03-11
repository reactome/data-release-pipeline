package org.reactome.release.dataexport;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.io.FileMatchers.aFileWithSize;
import static org.reactome.release.dataexport.DataExportUtilities.*;

public class DataExportUtilitiesTest {
	private final Path TEST_FILE = Paths.get("src", "main", "resources", "created_test_file.txt");
	private final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
	private Set<String> stringSet;

	@BeforeEach
	public void initializeSet() {
		this.stringSet = new HashSet<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h"));
	}

	@Test
	public void splitSetWithEvenSubSetSizes() {
		final int EXPECTED_NUMBER_OF_SUBSETS = 4;
		final int EXPECTED_NUMBER_OF_ITEMS_PER_SUBSET = 2;

		List<Set<String>> listOfStringSubSets = splitSet(stringSet, EXPECTED_NUMBER_OF_SUBSETS);

		assertThat(listOfStringSubSets, hasSize(EXPECTED_NUMBER_OF_SUBSETS));
		assertThat(listOfStringSubSets, everyItem(hasSize(EXPECTED_NUMBER_OF_ITEMS_PER_SUBSET)));
	}

	@Test
	public void splitSetWithUnEvenSubSetSizes() {
		final int EXPECTED_NUMBER_OF_SUBSETS = 3;
		final int EXPECTED_NUMBER_OF_ITEMS_PER_SUBSET = 3;
		final int EXPECTED_NUMBER_OF_ITEMS_IN_LAST_SUBSET = 2;

		List<Set<String>> listOfStringSubSets = splitSet(stringSet, EXPECTED_NUMBER_OF_SUBSETS);

		assertThat(listOfStringSubSets, hasSize(EXPECTED_NUMBER_OF_SUBSETS));

		// Three sets of size 3, 3, and 2
		assertThat(listOfStringSubSets.get(0), hasSize(EXPECTED_NUMBER_OF_ITEMS_PER_SUBSET));
		assertThat(listOfStringSubSets.get(1), hasSize(EXPECTED_NUMBER_OF_ITEMS_PER_SUBSET));
		assertThat(listOfStringSubSets.get(2), hasSize(EXPECTED_NUMBER_OF_ITEMS_IN_LAST_SUBSET));
	}

	@Test
	public void createsXMLDocument() throws ParserConfigurationException {
		final String EXPECTED_XML_VERSION = "1.0";

		Document document = createXMLDocument();

		assertThat(document.getXmlStandalone(), is(true));
		assertThat(document.getXmlVersion(), is(equalTo(EXPECTED_XML_VERSION)));
	}

	@Test
	public void emptyXMLDocumentCreatesXMLString() throws ParserConfigurationException, TransformerException {
		Document document = createXMLDocument();

		String expectedXML = String.format("%s%n%n", XML_HEADER);

		assertThat(transformDocumentToXMLString(document), is(equalTo(expectedXML)));
	}

	@Test
	public void populatedXMLDocumentCreatesXMLString() throws ParserConfigurationException, TransformerException {
		final String INDENT = String.format("%4s", "");
		final String ROOT_ELEMENT_NAME = "root";
		final String CHILD_ELEMENT_NAME = "name";
		final String CHILD_ELEMENT_TEXT = "inner text";

		final String EXPECTED_XML = String.join(System.lineSeparator(),
			XML_HEADER,
			String.format("<%s>", ROOT_ELEMENT_NAME),
			String.format("%s<%s>%s</%s>", INDENT, CHILD_ELEMENT_NAME, CHILD_ELEMENT_TEXT, CHILD_ELEMENT_NAME),
			String.format("</%s>%n", ROOT_ELEMENT_NAME)
		);

		Document document = createXMLDocument();
		Element rootElement = attachRootElement(document, ROOT_ELEMENT_NAME);
		Element childElement = getElement(document, CHILD_ELEMENT_NAME, CHILD_ELEMENT_TEXT);
		rootElement.appendChild(childElement);

		assertThat(transformDocumentToXMLString(document), is(equalTo(EXPECTED_XML)));
	}

	@Test
	public void createNewFile() throws IOException {
		deleteAndCreateFile(TEST_FILE);

		assertThat(TEST_FILE.toFile(), aFileWithSize(0));
	}

	@Test
	public void overwriteExistingFile() throws IOException {
		final String DUMMY_TEXT = "test text";

		// Create file with some text and assert the text is written
		deleteAndCreateFile(TEST_FILE);
		Files.write(TEST_FILE, DUMMY_TEXT.getBytes(), StandardOpenOption.APPEND);
		assertThat(TEST_FILE.toFile(), aFileWithSize(greaterThan(0L)));

		// Delete and re-create the file and assert it is empty
		deleteAndCreateFile(TEST_FILE);
		assertThat(TEST_FILE.toFile(), aFileWithSize(0));
	}

	@Test
	public void appendsOneLine() throws IOException {
		final String DUMMY_TEXT_LINE = "Some dummy text to append";

		deleteAndCreateFile(TEST_FILE);
		appendWithNewLine(DUMMY_TEXT_LINE, TEST_FILE);
		String fileContent = FileUtils.readFileToString(TEST_FILE.toFile(), Charset.defaultCharset());

		assertThat(fileContent, is(equalTo(DUMMY_TEXT_LINE.concat(System.lineSeparator()))));
	}

	@Test
	public void appendsMultipleLines() throws IOException {
		final List<String> DUMMY_TEXT_LINES = Arrays.asList(
			"Dummy text line 1",
			"Dummy text line 2",
			"Dummy text line 3"
		);

		final String EXPECTED_TEST_FILE_TEXT =
			"Dummy text line 1" + System.lineSeparator() +
			"Dummy text line 2" + System.lineSeparator() +
			"Dummy text line 3" + System.lineSeparator();

		deleteAndCreateFile(TEST_FILE);
		appendWithNewLine(DUMMY_TEXT_LINES, TEST_FILE);

		assertThat(Files.lines(TEST_FILE).count(), is(equalTo((long) DUMMY_TEXT_LINES.size())));

		String fileContent = FileUtils.readFileToString(TEST_FILE.toFile(), Charset.defaultCharset());
		assertThat(fileContent, is(equalTo(EXPECTED_TEST_FILE_TEXT)));
	}

	@AfterEach
	public void deleteTestFile() throws IOException {
		Files.deleteIfExists(TEST_FILE);
	}
}
