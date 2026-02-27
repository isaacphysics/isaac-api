package uk.ac.cam.cl.dtg.isaac.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Figure;
import uk.ac.cam.cl.dtg.isaac.dos.content.FigureRegion;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacDndValidatorTest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class ContentValidatorUtilsTest {

    private static final Supplier<GetDropZonesTestCase> invalidDropZone = () ->
            new GetDropZonesTestCase().expectDropZones();

    static Stream<GetDropZonesTestCase> getDropZonesTestCases() {
        return Stream.of(
                invalidDropZone.get().setChildren(List.of(new Content(""))),
                invalidDropZone.get().setChildren(List.of(new Content("no drop zone"))),
                invalidDropZone.get().setChildren(List.of(new Content("[drop-zone A1]"))),
                invalidDropZone.get().setChildren(List.of(new Content("[drop-zone: A1]"))),
                invalidDropZone.get().setChildren(List.of(new Content("[drop-zone:A1 | w-100]"))),
                invalidDropZone.get().setChildren(List.of(new Content("[drop-zone:A1|w-100 h-50]"))),
                invalidDropZone.get().setChildren(List.of(new Content("[drop-zone:A1|h-100w-50]"))),

                new GetDropZonesTestCase().setTitle("noContent_noDropZones").setChildren(null).expectDropZones(),

                new GetDropZonesTestCase().setTitle("singleDropZoneSingleText_returnsDropZone")
                        .setChildren(List.of(new Content("[drop-zone:A1]")))
                        .expectDropZones("A1"),

                new GetDropZonesTestCase().setTitle("singleDropZoneSingleContent_returnsDropZone")
                        .setChildren(List.of(new Content("[drop-zone:A1|w-100]")))
                        .expectDropZones("A1"),

                new GetDropZonesTestCase().setTitle("singleDropZoneSingleContentHeight_returnsDropZone")
                        .setChildren(List.of(new Content("[drop-zone:A1|h-100]")))
                        .expectDropZones("A1"),

                new GetDropZonesTestCase().setTitle("singleDropZoneSingleContentWidthHeight_returnsDropZone")
                        .setChildren(List.of(new Content("[drop-zone:A1|w-100h-50]")))
                        .expectDropZones("A1"),

                new GetDropZonesTestCase().setTitle("singleDropZoneSingleContentWithinLatex_returnsDropZone")
                        .setChildren(List.of(new Content("$$1 + \\text{[drop-zone:A1]}$$")))
                        .expectDropZones("A1"),

                new GetDropZonesTestCase().setTitle("multiDropZoneSingleContent_returnsDropZones")
                        .setChildren(List.of(new Content("Some text [drop-zone:A1], other text [drop-zone:A2]")))
                        .expectDropZones("A1", "A2"),

                new GetDropZonesTestCase().setTitle("multiDropZoneMultiContent_returnsDropZones")
                        .setChildren(List.of(
                                new Content("[drop-zone:A1] [drop-zone:A2]"),
                                new Content("[drop-zone:A3] [drop-zone:A4]")
                        )).expectDropZones("A1", "A2", "A3", "A4"),

                new GetDropZonesTestCase().setTitle("singleDropZoneNestedContent_returnsDropZones")
                        .setChildren(new LinkedList<>(List.of(new Content(), new Content("[drop-zone:A2]"))))
                        .tapQuestion(q -> ((Content) q.getChildren().get(0)).setChildren(List.of(new Content("[drop-zone:A1]"))))
                        .expectDropZones("A1", "A2"),

                new GetDropZonesTestCase().setTitle("figureContentWithoutDropZones_returnsNoZones")
                        .setChildren(List.of(new Figure()))
                        .expectDropZones(),

                new GetDropZonesTestCase().setTitle("figureContent_returnsDropZones")
                        .setChildren(List.of(createFigure("A1", "A2")))
                        .expectDropZones("A1", "A2"),

                new GetDropZonesTestCase().setTitle("mixedButNoNesting_returnsDropZones")
                        .setChildren(new LinkedList<>(List.of(createFigure("A1", "A2"), new Content("[drop-zone:A3]"))))
                        .expectDropZones("A1", "A2", "A3"),

                new GetDropZonesTestCase().setTitle("mixedNested_returnsDropZones")
                        .setChildren(new LinkedList<>(List.of(new Content(), new Content("[drop-zone:A2]"))))
                        .tapQuestion(q -> {
                            Content content = (Content) q.getChildren().get(0);
                            content.setChildren(List.of(
                                    new Content("[drop-zone:A1]"),
                                    createFigure("F1", "F2")
                            ));
                        }).expectDropZones("A1", "F1", "F2", "A2")
        );
    }

    @ParameterizedTest
    @MethodSource("getDropZonesTestCases")
    public void testGetDropZones(final GetDropZonesTestCase testCase) {
        List<String> dropZones = ContentValidatorUtils.DropZones.getFromQuestion(testCase.question);
        assertEquals(testCase.dropZones, dropZones);
    }

    private static Figure createFigure(final String... dropZones) {
        Figure figure = new Figure();
        figure.setFigureRegions(new ArrayList<>());
        for (String dropZoneId : dropZones) {
            FigureRegion region = new FigureRegion();
            region.setId(dropZoneId);
            figure.getFigureRegions().add(region);
        }
        return figure;
    }

    @SuppressWarnings("checkstyle:MissingJavadocType")
    public static class GetDropZonesTestCase extends IsaacDndValidatorTest.TestCase<GetDropZonesTestCase> {}
}