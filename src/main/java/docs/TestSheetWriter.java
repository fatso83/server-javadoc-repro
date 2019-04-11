package docs;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

class TestSheetWriter {

    private static final String ns = "urn:schemas-microsoft-com:office:spreadsheet";

    private static final Style inputCellStyle = new Style(false, false, "#000000", null);
    private static final Style labelCellStyle = new Style(true, false, "#FFFFFF", "#5ba49a");
    private static final Style centeredLabelCellStyle = new Style(true, true, "#FFFFFF", "#5ba49a");

    private final TestSheetDoclet.TestGroups testGroups;

    TestSheetWriter(TestSheetDoclet.TestGroups testGroups) {
        this.testGroups = requireNonNull(testGroups);
    }

    /**
     * Write Excel XML in accordance with https://docs.microsoft.com/en-us/previous-versions/office/developer/office-xp/aa140066(v=office.10)
     *
     * @param filename The filename to write the generated Excel file to
     */
    void write(String filename) {

        final var dbf = DocumentBuilderFactory.newInstance();
        try {
            final var document = dbf.newDocumentBuilder().newDocument();
            final var workbook = document.createElementNS(ns, "Workbook");
            final var styles = document.createElementNS(ns, "Styles");
            styles.appendChild(createStyleElement(document, inputCellStyle));
            styles.appendChild(createStyleElement(document, labelCellStyle));
            styles.appendChild(createStyleElement(document, centeredLabelCellStyle));
            workbook.appendChild(styles);

            for (TestSheetDoclet.TestGroup testGroup : testGroups.testGroups) {
                final var worksheet = document.createElementNS(ns, "Worksheet");
                worksheet.setAttributeNS(ns, "Name", String.format("%s - %s", testGroup.id, testGroup.name));
                final var table = document.createElementNS(ns, "Table");

                // Create 5 columns of appropriate width
                for (int i = 0; i < 5; i++) {
                    final var column = document.createElementNS(ns, "Column");

                    final var multiplier = 4.5; // This was found by trial-and-error and depends on font type
                    switch (i) {
                        case 1:
                            column.setAttributeNS(ns, "Width", String.valueOf(getMaxColumnWidth(testGroup, step1 -> step1.action.length()) * multiplier));
                            break;
                        case 2:
                            column.setAttributeNS(ns, "Width", String.valueOf(getMaxColumnWidth(testGroup, step -> step.expectedResults.stream().mapToInt(String::length).max().orElse(0)) * multiplier));
                            break;
                        case 4:
                            column.setAttributeNS(ns, "Width", "200"); // The "Comment" column has double width
                            break;
                        default:
                            column.setAttributeNS(ns, "Width", "100");
                            break;

                    }
                    table.appendChild(column);
                }

                for (TestSheetDoclet.TestSpecification test : testGroup.tests) {
                    table.appendChild(
                            createRow(document,
                                    createStringCell(document, "Test number", labelCellStyle),
                                    createStringCell(document, test.id, inputCellStyle, 2),
                                    createStringCell(document, "Tested by", labelCellStyle),
                                    createStringCell(document, "", inputCellStyle)
                            )
                    );

                    table.appendChild(
                            createRow(document,
                                    createStringCell(document, "Test name", labelCellStyle),
                                    createStringCell(document, test.name, inputCellStyle, 2),
                                    createStringCell(document, "Tested on", labelCellStyle),
                                    createStringCell(document, "", inputCellStyle)
                            )
                    );

                    table.appendChild(
                            createRow(document,
                                    createStringCell(document, "Precondition", labelCellStyle),
                                    createStringCell(document, "", labelCellStyle, 4)
                            )
                    );

                    int preCondNum = 1;
                    for (String precondition : test.preconditions) {
                        table.appendChild(
                                createRow(document,
                                        createStringCell(document, String.valueOf(preCondNum++), centeredLabelCellStyle),
                                        createStringCell(document, precondition, inputCellStyle, 2),
                                        createStringCell(document, "", labelCellStyle, 2)
                                )
                        );
                    }

                    final var stepRow = createRow(document,
                            createStringCell(document, "Step", labelCellStyle),
                            createStringCell(document, "Action", labelCellStyle),
                            createStringCell(document, "Expected result", labelCellStyle),
                            createStringCell(document, "Pass/fail", labelCellStyle),
                            createStringCell(document, "Comment", labelCellStyle)
                    );
                    table.appendChild(stepRow);

                    int stepNum = 1;
                    for (TestSheetDoclet.TestStep step : test.steps) {
                        final var row = createRow(document,
                                createStringCell(document, String.valueOf(stepNum++), centeredLabelCellStyle),
                                createStringCell(document, step.action, inputCellStyle),
                                createStringCell(document, "", inputCellStyle),
                                createStringCell(document, "", inputCellStyle)
                        );
                        table.appendChild(row);

                        if (step.expectedResults.size() > 1) {
                            for (String expectedResult : step.expectedResults.subList(1, step.expectedResults.size())) {
                                final var expResRow = createRow(document,
                                        createStringCell(document, "", centeredLabelCellStyle),
                                        createStringCell(document, "", inputCellStyle),
                                        createStringCell(document, expectedResult, inputCellStyle),
                                        createStringCell(document, "", inputCellStyle),
                                        createStringCell(document, "", inputCellStyle)
                                );
                                table.appendChild(expResRow);

                            }
                        }
                    }

                    // Two empty rows as separators between tests
                    for (int i = 0; i < 2; i++) {
                        table.appendChild(
                                createRow(document,
                                        createEmptyCell(document),
                                        createEmptyCell(document),
                                        createEmptyCell(document),
                                        createEmptyCell(document),
                                        createEmptyCell(document)
                                )
                        );
                    }
                }

                worksheet.appendChild(table);
                workbook.appendChild(worksheet);
            }
            document.appendChild(workbook);
            prettyPrint(document, filename);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    private int getMaxColumnWidth(TestSheetDoclet.TestGroup testGroup, Function<TestSheetDoclet.TestStep, Integer> propertyMapping) {
        return testGroup.tests.stream().mapToInt(test -> test.steps.stream().mapToInt(propertyMapping::apply).max().orElse(0)).max().orElse(0);
    }

    private Node createStyleElement(Document document, Style style) {
        final var styleElement = document.createElementNS(ns, "Style");
        styleElement.setAttributeNS(ns, "ID", style.id);

        final var fontElement = document.createElementNS(ns, "Font");
        if (style.boldText) {
            fontElement.setAttributeNS(ns, "Bold", "1");
        }
        fontElement.setAttributeNS(ns, "FontName", "Calibri");
        fontElement.setAttributeNS(ns, "Color", style.fontColor);
        styleElement.appendChild(fontElement);

        if (style.centeredText) {
            final var alignmentElement = document.createElementNS(ns, "Alignment");
            alignmentElement.setAttributeNS(ns, "Horizontal", "Center");
            styleElement.appendChild(alignmentElement);
        }

        if (style.backgroundColor != null) {
            final var interiorElement = document.createElementNS(ns, "Interior");
            interiorElement.setAttributeNS(ns, "Color", style.backgroundColor);
            interiorElement.setAttributeNS(ns, "Pattern", "Solid");
            styleElement.appendChild(interiorElement);
        }

        return styleElement;
    }

    private Element createRow(Document doc, Element... cells) {
        final var row = doc.createElementNS(ns, "Row");
        for (Element cell : cells) {
            row.appendChild(cell);
        }
        return row;
    }

    private Element createEmptyCell(Document doc) {
        return doc.createElementNS(ns, "Cell");
    }

    private Element createStringCell(Document doc, String text, Style style) {
        return createStringCell(doc, text, style, 1);
    }

    private Element createStringCell(Document doc, String text, Style style, int span) {
        final var cell = doc.createElementNS(ns, "Cell");
        if (span > 1) {
            cell.setAttributeNS(ns, "MergeAcross", String.format("%d", span - 1));
        }
        final var data = doc.createElementNS(ns, "Data");
        data.setAttributeNS(ns, "Type", "String");
        data.setTextContent(text);
        cell.setAttributeNS(ns, "StyleID", style.id);
        cell.appendChild(data);

        return cell;
    }

    private void prettyPrint(Document doc, String filename) throws TransformerException, IOException {
        final var outputStream = new FileOutputStream(filename);
        final var printStream = new PrintStream(outputStream);

        // Required processing instructions
        printStream.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
        printStream.println("<?mso-application progid=\"Excel.Sheet\"?>");

        final TransformerFactory tf = TransformerFactory.newInstance();
        final Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(outputStream, UTF_8)));

        outputStream.close();
    }

    static class Style {

        static int idSequence = 1;

        final String id;
        final boolean boldText;
        final boolean centeredText;
        final String fontColor;
        final String backgroundColor;

        Style(boolean boldText, boolean centeredText, String fontColor, String backgroundColor) {
            this.id = String.valueOf(Style.idSequence++);
            this.boldText = boldText;
            this.centeredText = centeredText;
            this.fontColor = fontColor;
            this.backgroundColor = backgroundColor;
        }
    }
}
