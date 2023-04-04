package saka1029.pdf.itext;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Test;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class TestIText {

    static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

    public record Text(float x, float y, float width, float height, String text) implements Comparable<Text> {
        @Override
        public String toString() {
            return "%.0fx%.0f:%.0fx%.0f:%s".formatted(x, y, width, height, text);
        }

        @Override
        public int compareTo(Text o) {
            int result = Float.compare(x, o.x);
            if (result == 0)
                result = Float.compare(o.y, y);
            return result;
        }
    }

    public static class IText {
        /**
         * A4のポイントサイズ 横約8.27 × 縦約11.69 インチ 595.44 x 841.68 ポイント
         */
        static final float PAGE_WIDTH = 596F, PAGE_HEIGHT = 842F;

        static float round(float f) {
            return Math.round(f);
        }

        static Text newText(TextRenderInfo info, boolean horizontal) {
            Rectangle2D.Float baseBox = info.getBaseline().getBoundingRectange();
            Rectangle2D.Float ascentBox = info.getAscentLine().getBoundingRectange();
            return new Text(
                round(horizontal ? baseBox.x : PAGE_HEIGHT - baseBox.y),
                round(horizontal ? PAGE_HEIGHT - baseBox.y : PAGE_HEIGHT - baseBox.x),
                round(baseBox.width),
                round(ascentBox.y - baseBox.y),
                info.getText());
        }

        public static List<NavigableMap<Float, NavigableSet<Text>>> read(String filename, boolean horizontal)
            throws IOException {
            List<NavigableMap<Float, NavigableSet<Text>>> result = new ArrayList<>();
            PdfReader reader = new PdfReader(filename);
            try (Closeable c = () -> reader.close()) {
                int numberOfPages = reader.getNumberOfPages();
                PdfReaderContentParser parser = new PdfReaderContentParser(reader);
                for (int i = 1; i <= numberOfPages; ++i) {
                    NavigableMap<Float, NavigableSet<Text>> page = new TreeMap<>();
                    result.add(page);
                    parser.processContent(i, new TextExtractionStrategy() {
                        @Override
                        public void renderText(TextRenderInfo renderInfo) {
                            Text text = newText(renderInfo, horizontal);
                            page.computeIfAbsent(text.y, k -> new TreeSet<>()).add(text);
                        }

                        @Override
                        public void renderImage(ImageRenderInfo renderInfo) {
                        }

                        @Override
                        public void endTextBlock() {
                        }

                        @Override
                        public void beginTextBlock() {
                        }

                        @Override
                        public String getResultantText() {
                            return null;
                        }
                    });
                }
            }
            return result;
        }
    }

    static void testRead(String filename) throws IOException {
        List<NavigableMap<Float, NavigableSet<Text>>> pages = IText.read(filename, true);
        for (int i = 0, size = pages.size(); i < size; ++i) {
            OUT.printf("#file: %s page: %d%n", filename, i + 1);
            for (NavigableSet<Text> e : pages.get(i).values())
                for (Text t : e)
                    if (!t.text.isBlank())
                        OUT.println(t);
        }
    }

    @Test
    public void testRead() throws IOException {
//        testRead("data/tuti.pdf");
        testRead("data/kokuji.pdf");
    }
}
