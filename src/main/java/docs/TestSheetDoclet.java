package docs;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.sun.source.doctree.DocTree.Kind.UNKNOWN_BLOCK_TAG;

public class TestSheetDoclet implements Doclet {

    private final TestGroups testGroups = new TestGroups();

    @Override
    public void init(Locale locale, Reporter reporter) {
    }

    @Override
    public String getName() {
        return "TestSheetDoclet";
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean run(DocletEnvironment docEnv) {
        final DocTrees docTrees = docEnv.getDocTrees();

        for (Element element : docEnv.getSpecifiedElements()) {
            element.accept(new ElementVisitor<Void, Void>() {
                @Override
                public Void visit(Element e, Void aVoid) {
                    e.getEnclosedElements().forEach(enc -> enc.accept(this, null));
                    return null;
                }

                @Override
                public Void visitPackage(PackageElement e, Void aVoid) {
                    e.getEnclosedElements().forEach(enc -> enc.accept(this, null));
                    return null;
                }

                @Override
                public Void visitType(TypeElement e, Void aVoid) {
                    parseClassDocs(docTrees.getDocCommentTree(e)).ifPresent(testGroups::addTestGroup);
                    e.getEnclosedElements().forEach(enc -> enc.accept(this, null));
                    return null;
                }

                @Override
                public Void visitVariable(VariableElement e, Void aVoid) {
                    e.getEnclosedElements().forEach(enc -> enc.accept(this, null));
                    return null;
                }

                @Override
                public Void visitExecutable(ExecutableElement e, Void aVoid) {
                    parseMethodDocs(docTrees.getDocCommentTree(e)).ifPresent(testGroups::addTestSpecification);
                    e.getEnclosedElements().forEach(enc -> enc.accept(this, null));
                    return null;
                }

                @Override
                public Void visitTypeParameter(TypeParameterElement e, Void aVoid) {
                    e.getEnclosedElements().forEach(enc -> enc.accept(this, null));
                    return null;
                }

                @Override
                public Void visitUnknown(Element e, Void aVoid) {
                    e.getEnclosedElements().forEach(enc -> enc.accept(this, null));
                    return null;
                }
            }, null);
        }

        final var outputFile = String.format("%s-nimble-acceptance-tests.xml", getDate());
        final var testSheetWriter = new JacksonWriter(testGroups);
        testSheetWriter.write(outputFile);
        System.out.println("Wrote acceptance test sheet to: " + Paths.get(outputFile).toAbsolutePath());

        return true;
    }

    private String getDate() {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return formatter.format(ZonedDateTime.now());
    }

    private Optional<TestGroup> parseClassDocs(DocCommentTree dcTree) {
        if (dcTree == null) {
            return Optional.empty();
        }

        final List<? extends DocTree> tags = dcTree.getBlockTags();
        if (tags.isEmpty()) {
            return Optional.empty();
        }

        final var testGroup = new TestGroup();
        tags.forEach(tag -> {
            if (tag.getKind() == UNKNOWN_BLOCK_TAG) {
                final var unknown = (UnknownBlockTagTree) tag;
                final var tagName = unknown.getTagName();
                final var content = unknown.getContent().stream().map(obj -> obj.toString().replaceAll("\n", "")).collect(Collectors.joining(" "));

                switch (tagName) {
                    case "id":
                        testGroup.id = content;
                        break;
                    case "name":
                        testGroup.name = content;
                        break;
                }

            }
        });

        return testGroup.isComplete() ? Optional.of(testGroup) : Optional.empty();
    }

    private Optional<TestSpecification> parseMethodDocs(DocCommentTree dcTree) {
        if (dcTree == null) {
            return Optional.empty();
        }

        final List<? extends DocTree> tags = dcTree.getBlockTags();
        if (tags.isEmpty()) {
            return Optional.empty();
        }

        final var testSpecification = new TestSpecification();

        tags.forEach(tag -> {
            if (tag.getKind() == UNKNOWN_BLOCK_TAG) {
                final var unknown = (UnknownBlockTagTree) tag;
                final var tagName = unknown.getTagName();
                final var content = unknown.getContent().stream().map(obj -> obj.toString().replaceAll("\n", "")).collect(Collectors.joining(" "));
                switch (tagName) {
                    case "id":
                        testSpecification.id = content;
                        break;
                    case "name":
                        testSpecification.name = content;
                        break;
                    case "precondition":
                        testSpecification.preconditions.add(content);
                        break;
                    case "step":
                        final var testStep = new TestStep();
                        testStep.action = content;
                        testSpecification.steps.add(testStep);
                        break;
                    case "expectedResult":
                        break;
                }
            }
        });

        return testSpecification.isComplete() ? Optional.of(testSpecification) : Optional.empty();
    }

    static class TestGroups {

        List<TestGroup> testGroups = new ArrayList<>();
        TestGroup mostRecentTestGroup = null;

        void addTestGroup(TestGroup group) {
            mostRecentTestGroup = group;
            testGroups.add(group);
            testGroups.sort(Comparator.comparing(elem -> Integer.valueOf(elem.id)));
        }

        void addTestSpecification(TestSpecification testSpecification) {
            mostRecentTestGroup.tests.add(testSpecification);
        }
    }

    static class TestGroup {

        String id;
        String name;
        List<TestSpecification> tests = new ArrayList<>();

        boolean isComplete() {
            return true;
        }
    }

    static class TestSpecification {

        String id;
        String name;
        List<String> preconditions = new ArrayList<>();
        List<TestStep> steps = new ArrayList<>();

        public boolean isComplete() {
            return true;
        }
    }

    static class TestStep {

        String action;
        List<String> expectedResults = new ArrayList<>();

    }
}
