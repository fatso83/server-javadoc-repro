package docs;

import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import javax.xml.stream.XMLOutputFactory;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;

@SuppressWarnings("WeakerAccess")
public class JacksonWriter {

    private static final String NS = "urn:schemas-microsoft-com:office:spreadsheet";
    private TestSheetDoclet.TestGroups testGroups;

    public static void main(String[] args) throws JsonProcessingException {
        final var workbook = new Workbook();
        workbook.addStyle("123", new Font(true, "#123123", "Calibri"), null, null);
        workbook.addStyle("456", new Font(true, "#123123", "Calibri"), new Alignment("Center"), null);
        workbook.addStyle("789", new Font(true, "#123123", "Calibri"), new Alignment("Center"), new Interior("#CECECE", "Solid"));

        final var worksheet = workbook.addWorksheet("My worksheet");
        worksheet.addColumn(100);
        worksheet.addColumn(200);
        worksheet.addRow(
                new Cell("123", null, "Foo!"),
                new Cell("456", null, "Foo!"),
                new Cell("789", null, "Foo!"),
                new Cell("123", null, "Foo!"),
                new Cell("123", null, "Foo!"),
                new Cell("123", null, "Foo!"),
                new Cell("123", null, "Foo!"),
                new Cell(null, null, "Bar!")
        );
        worksheet.addRow(
                new Cell("123", 7, "Boom!")
        );

        final var xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        final var xmlMapper = new XmlMapper(xmlModule)
                .configure(FAIL_ON_EMPTY_BEANS, false)
                .enable(SerializationFeature.INDENT_OUTPUT);

        System.out.println("XMLOutputFactory: " + XMLOutputFactory.newInstance().getClass());

        final String xml = xmlMapper.writeValueAsString(workbook);
        System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
        System.out.println("<?mso-application progid=\"Excel.Sheet\"?>");
        System.out.println(xml);
    }

    JacksonWriter(TestSheetDoclet.TestGroups testGroups) {
        this.testGroups = testGroups;
    }

    void write(String filename) {
        final var inputCellStyle = "inputCellStyle";
        final var labelCellStyle = "labelCellStyle";
        final var centeredLabelCellStyle = "centeredLabelCellStyle";

        final var workbook = new Workbook();
        workbook.addStyle(inputCellStyle, new Font(false, "#000000", "Calibri"), null, null);
        workbook.addStyle(labelCellStyle, new Font(true, "#FFFFFF", "Calibri"), null, new Interior("#5ba49a", "Solid"));
        workbook.addStyle(centeredLabelCellStyle, new Font(true, "#FFFFFF", "Calibri"), new Alignment("Center"), new Interior("#5ba49a", "Solid"));

        for (TestSheetDoclet.TestGroup testGroup : testGroups.testGroups) {
            final var worksheet = workbook.addWorksheet(String.format("%s - %s", testGroup.id, testGroup.name));

            // Create 5 columns of appropriate width
            for (int i = 0; i < 5; i++) {
                worksheet.addColumn(100);
            }

            for (TestSheetDoclet.TestSpecification test : testGroup.tests) {
                worksheet.addRow(
                        new Cell(labelCellStyle, "Test number"),
                        new Cell(inputCellStyle, 2, test.id),
                        new Cell(labelCellStyle, "Tested by"),
                        new Cell(inputCellStyle, 2, "")
                );

                worksheet.addRow(
                        new Cell(labelCellStyle, "Test name"),
                        new Cell(inputCellStyle, 2, test.name),
                        new Cell(labelCellStyle, "Tested on"),
                        new Cell(inputCellStyle, 2, "")
                );

                worksheet.addRow(
                        new Cell(labelCellStyle, "Precondition"),
                        new Cell(labelCellStyle, 4, "")
                );

                int preCondNum = 1;
                for (String precondition : test.preconditions) {
                    worksheet.addRow(
                            new Cell(centeredLabelCellStyle, String.valueOf(preCondNum++)),
                            new Cell(inputCellStyle, 2, precondition),
                            new Cell(labelCellStyle, 2, "")
                    );
                }

                worksheet.addRow(
                        new Cell(labelCellStyle, "Step"),
                        new Cell(labelCellStyle, "Action"),
                        new Cell(labelCellStyle, "Expected result"),
                        new Cell(labelCellStyle, "Pass/fail"),
                        new Cell(labelCellStyle, "Comment")
                );

                int stepNum = 1;
                for (TestSheetDoclet.TestStep step : test.steps) {
                    worksheet.addRow(
                            new Cell(centeredLabelCellStyle, String.valueOf(stepNum++)),
                            new Cell(inputCellStyle, step.action),
                            new Cell(inputCellStyle, ""),
                            new Cell(inputCellStyle, "")
                    );

                    if (step.expectedResults.size() > 1) {
                        for (String expectedResult : step.expectedResults.subList(1, step.expectedResults.size())) {
                            worksheet.addRow(
                                    new Cell(centeredLabelCellStyle, ""),
                                    new Cell(inputCellStyle, ""),
                                    new Cell(inputCellStyle, expectedResult),
                                    new Cell(inputCellStyle, ""),
                                    new Cell(inputCellStyle, "")
                            );
                        }
                    }
                }

                // Two empty rows as separators between tests
                for (int i = 0; i < 2; i++) {
                    worksheet.addRow();
                }
            }
        }

        System.out.println(this.getClass().getClassLoader());
        System.out.println("Check that we can get hold of class: " + WstxOutputFactory.class.getName());

        // Set the property
        System.setProperty("javax.xml.stream.XMLOutputFactory", WstxOutputFactory.class.getName());

        // Prepare for crash with ClassNotFound ...
        System.out.println("XMLOutputFactory: " + XMLOutputFactory.newInstance().getClass());
//        System.setProperty("javax.xml.stream.isRepairingNamespaces", "true");
//        XMLOutputFactory.newInstance().setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);

        final var xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        final var xmlMapper = new XmlMapper(xmlModule)
                .configure(FAIL_ON_EMPTY_BEANS, false)
//                .enable(SerializationFeature.INDENT_OUTPUT)
                ;

        try {
            final var out = new PrintStream(filename);
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            out.println("<?mso-application progid=\"Excel.Sheet\"?>");
            out.println(xmlMapper.writeValueAsString(workbook));
            out.close();
        } catch (Exception err) {
            throw new RuntimeException(err);
        }

    }

    @JacksonXmlRootElement(localName = "Workbook", namespace = NS)
    public static class Workbook {
        @JacksonXmlProperty(localName = "Styles", namespace = NS)
        public Styles styles;
        @JacksonXmlProperty(localName = "Worksheet", namespace = NS)
        public List<Worksheet> worksheets = new ArrayList<>();

        void addStyle(String id, Font font, Alignment alignment, Interior interior) {
            if (styles == null) {
                styles = new Styles();
            }
            final var style = new Style();
            style.id = id;
            style.font = font;
            style.alignment = alignment;
            style.interior = interior;
            styles.styleList.add(style);
        }

        Worksheet addWorksheet(String name) {
            final var worksheet = new Worksheet(name);
            worksheets.add(worksheet);
            return worksheet;
        }
    }

    public static class Styles {
        @JacksonXmlProperty(localName = "Style", namespace = NS)
        public List<Style> styleList = new ArrayList<>();
    }

    public static class Style {
        @JacksonXmlProperty(localName = "ID", namespace = NS, isAttribute = true)
        public String id;

        @JacksonXmlProperty(localName = "Font", namespace = NS)
        public Font font;

        @JacksonXmlProperty(localName = "Alignment", namespace = NS)
        public Alignment alignment;

        @JacksonXmlProperty(localName = "Interior", namespace = NS)
        public Interior interior;
    }

    public static class Font {
        @JacksonXmlProperty(localName = "Bold", namespace = NS, isAttribute = true)
        public String bold;

        @JacksonXmlProperty(localName = "Color", namespace = NS, isAttribute = true)
        public String color;

        @JacksonXmlProperty(localName = "FontName", namespace = NS, isAttribute = true)
        public String fontName;

        public Font(boolean bold, String color, String fontName) {
            this.bold = bold ? "1" : "0";
            this.color = color;
            this.fontName = fontName;
        }
    }

    public static class Alignment {
        @JacksonXmlProperty(localName = "Horizontal", namespace = NS, isAttribute = true)
        public String horizontal;

        public Alignment(String horizontal) {
            this.horizontal = horizontal;
        }
    }

    public static class Interior {
        @JacksonXmlProperty(localName = "Color", namespace = NS, isAttribute = true)
        public String color;

        @JacksonXmlProperty(localName = "Pattern", namespace = NS, isAttribute = true)
        public String pattern;

        public Interior(String color, String pattern) {
            this.color = color;
            this.pattern = pattern;
        }
    }

    public static class Worksheet {
        @JacksonXmlProperty(localName = "Name", namespace = NS, isAttribute = true)
        public String name;

        @JacksonXmlProperty(localName = "Table", namespace = NS)
        public Table table;

        public Worksheet(String name) {
            this.name = name;
        }

        void addColumn(int width) {
            final var column = new Column();
            column.width = String.valueOf(width);
            if (table == null) {
                table = new Table();
            }
            table.columns.add(column);
        }

        void addRow(Cell... cells) {
            final var row = new Row();
            row.cells.addAll(Arrays.asList(cells));
            if (table == null) {
                table = new Table();
            }
            table.rows.add(row);
        }
    }

    public static class Table {
        @JacksonXmlProperty(localName = "Column", namespace = NS)
        public List<Column> columns = new ArrayList<>();

        @JacksonXmlProperty(localName = "Row", namespace = NS)
        public List<Row> rows = new ArrayList<>();
    }

    public static class Column {
        @JacksonXmlProperty(localName = "Width", namespace = NS, isAttribute = true)
        public String width;
    }

    public static class Row {
        @JacksonXmlProperty(localName = "Cell", namespace = NS)
        public List<Cell> cells = new ArrayList<>();
    }

    public static class Cell {
        @JacksonXmlProperty(localName = "StyleID", namespace = NS, isAttribute = true)
        public String styleId;

        @JacksonXmlProperty(localName = "MergeAcross", namespace = NS, isAttribute = true)
        public String mergeAcross;

        @JacksonXmlProperty(localName = "Data", namespace = NS)
        public Data data;

        public Cell(String styleId, String content) {
            this(styleId, null, content);
        }

        public Cell(String styleId, Integer mergeAcross, String content) {
            this.styleId = styleId;
            this.mergeAcross = mergeAcross == null ? null : String.valueOf(mergeAcross);
            this.data = new Data(content);
        }
    }

    public static class Data {
        @JacksonXmlProperty(localName = "Type", namespace = NS, isAttribute = true)
        public String type = "String";

        @JacksonXmlText
        public String content;

        public Data(String content) {
            this.content = content;
        }
    }
}
