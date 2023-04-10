package saka1029.pdf;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class IText {

    public static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

    /**
     * A4のポイントサイズ 横約8.27 × 縦約11.69 インチ 595.44 x 841.68 ポイント
     */
    public static final float PAGE_WIDTH = 596F, PAGE_HEIGHT = 842F;

    record Text(float x, float y, float w, float h, String text) {
        @Override
        public String toString() {
            return x + "x" + y + ":" + w + "x" + h + ":" + text;
        }
    }

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
                String text = info.getText();
                if (text.isBlank())
                    return;
                Rectangle2D.Float baseBox = info.getBaseline().getBoundingRectange();
                float ascent = info.getAscentLine().getBoundingRectange().y;
                float descent = info.getDescentLine().getBoundingRectange().y;
                page.add(new Text(
                    round(horizontal ? baseBox.x : PAGE_HEIGHT - baseBox.y),
                    round(horizontal ? PAGE_HEIGHT - baseBox.y : PAGE_WIDTH - baseBox.x),
                    round(baseBox.width),
                    round(ascent - descent),
                    text));
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

    static final Entry<Float, Integer> 既定の文字サイズ = Map.entry(10F, 0);

    /**
     * Text群における最頻文字サイズを求めます。
     * Textにおける文字の長さをテキストの高さで集約し、
     * 最も出現回数の多いテキストの高さを返します。
     * @param in Textのストリームを指定します。
     * @return 最頻文字サイズをポイントサイズで返します。
     *         ストリームがからの場合は10Fを返します。
     */
    public static float frequentCharHeight(Stream<Text> texts) {
    	return texts
    	    .collect(Collectors.groupingBy(text -> text.h(),
    	        Collectors.summingInt(text -> text.text().length()))) // 文字列の長さをテキストの高さで集計する。
			.entrySet().stream()
    		.max(Entry.comparingByValue())
    		.orElse(既定の文字サイズ)
    		.getKey();
    }

    /**
     * 1行分のTextのストリームを文字列に変換します。
     * @param line 1行分のTextのストリームを指定します。
     * @param leftMargin レフトマージンをポイントサイズで指定します。
     * 					 このサイズ分の行頭のスペースを無視します。
     * @param charWidth  半角スペースの幅をポイントサイズで指定します。
     * @return 文字列に変換した結果を返します。
     */
    public static String toString(Collection<Text> line, float leftMargin, float charWidth) {
        StringBuilder sb = new StringBuilder();
        float halfWidth = charWidth / 2;
        float start = leftMargin;
        for (Text text : line) {
            int spaces = Math.round((text.x - start) / halfWidth);
            for (int i = 0; i < spaces; ++i)
                sb.append(" ");
            sb.append(text.text());
            start = text.x + text.w;
        }
        return sb.toString();
    }


    public static List<List<String>> 読み込み(String filename, boolean horizontal) throws IOException {
        List<List<String>> result = new ArrayList<>();
        PdfReader reader = new PdfReader(filename);
        try (Closeable c = () -> reader.close()) {
            int numberOfPages = reader.getNumberOfPages();
            PdfReaderContentParser parser = new PdfReaderContentParser(reader);
            for (int pageNo = 1; pageNo <= numberOfPages; ++pageNo) {
                List<Text> page = new ArrayList<>();
                parse(parser, horizontal, pageNo, page);
                Collections.sort(page, Comparator.comparing(Text::y).thenComparing(Text::x));
                float leftMargin = page.stream()
                    .collect(Collectors.groupingBy(Text::h,
                        Collectors.summingInt(t -> t.text.length())))
                    .entrySet().stream()
                    .max(Entry.comparingByValue())
                    .orElse(既定の文字サイズ)
                    .getKey();
                float freqHeight = frequentCharHeight(page.stream());
                OUT.println("#file " + filename + " page=" + pageNo);
                for (Text text : page)
                    OUT.println(toString(List.of(text), leftMargin, freqHeight));
            }
        }
        return result;
    }

}
