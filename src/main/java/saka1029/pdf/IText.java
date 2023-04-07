package saka1029.pdf;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class IText {

    /**
     * A4のポイントサイズ 横約8.27 × 縦約11.69 インチ 595.44 x 841.68 ポイント
     */
    public static final float PAGE_WIDTH = 596F, PAGE_HEIGHT = 842F;

    static float round(float f) {
        return Math.round(f);
    }

    /**
     * 横書きの場合はx座標はそのまま、y座標は反転します。
     * 縦書きの場合はx座標は反転、y座標も反転します。
     * 幅(w)はbaseline.boundingRectanbleのwidthです。
     * 高さ(h)はアセントライン-ディセントラインです。
     * @param parser
     * @param horizontal
     * @param pageNo
     * @param page
     * @throws IOException
     */
    static void parse(PdfReaderContentParser parser, boolean horizontal, int pageNo, List<Text> page)
        throws IOException {
        parser.processContent(pageNo, new RenderListener() {
            public void renderText(TextRenderInfo info) {
                Rectangle2D.Float baseBox = info.getBaseline().getBoundingRectange();
                float ascent = info.getAscentLine().getBoundingRectange().y;
                float descent = info.getDescentLine().getBoundingRectange().y;
                page.add(new Text(
                    round(horizontal ? baseBox.x : PAGE_HEIGHT - baseBox.y),
                    round(horizontal ? PAGE_HEIGHT - baseBox.y : PAGE_WIDTH - baseBox.x),
                    round(baseBox.width),
                    round(ascent - descent),
                    info.getText()));
            }

            @Override
            public void beginTextBlock() {
            }

            @Override
            public void endTextBlock() {
            }

            @Override
            public void renderImage(ImageRenderInfo renderInfo) {
            }
        });
    }

    static final Comparator<Text> COMPARE_TEXT = (a, b) -> {
        int r = Float.compare(a.x(), b.y());
        if (r == 0)
            r = Float.compare(b.y(), a.y());
        return r;
    };

    public static List<List<String>> 読み込み(String filename, boolean horizontal) throws IOException {
        List<List<String>> result = new ArrayList<>();
        PdfReader reader = new PdfReader(filename);
        try (Closeable c = () -> reader.close()) {
            int numberOfPages = reader.getNumberOfPages();
            PdfReaderContentParser parser = new PdfReaderContentParser(reader);
            for (int pageNo = 1; pageNo <= numberOfPages; ++pageNo) {
                List<Text> page = new ArrayList<>();
                parse(parser, horizontal, pageNo, page);
                TreeMap<Float, List<Text>> aligned = page.stream()
                    .collect(Collectors.groupingBy(Text::y, TreeMap::new, Collectors.toList()));
            }
        }
        return result;
    }

}
