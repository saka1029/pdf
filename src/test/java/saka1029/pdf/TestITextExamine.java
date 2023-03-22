package saka1029.pdf;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class TestITextExamine {

    /*
     * 【横書きPDFファイルの場合】
     * Rectanble2D.Float(float x, float y, float height, float width)
     * ページのx座標は横方向
     * ページのy座標は縦方向
     * heightは常にゼロ
     * widthはほぼ文字のポイントサイズ。ただし半角は半分の幅になる。
     */
    static final PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

    static class MyTextExtractionStrategy implements TextExtractionStrategy {
        final PrintWriter out;
        final int page;

        MyTextExtractionStrategy(PrintWriter out, int page) {
            this.out = out;
            this.page = page;
        }

        @Override
        public void renderText(TextRenderInfo renderInfo) {
            Rectangle2D.Float box = renderInfo.getAscentLine().getBoundingRectange();
            out.printf("%d:%fx%f(%fx%f):'%s'%n",
                page, box.x, box.y, box.height, box.width, renderInfo.getText());
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

        @Override
        public String getResultantText() {
            return null;
        }
    }

    static void examine(String pdfFile, String outFile) throws IOException {
        PdfReader reader = new PdfReader(pdfFile);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        try (Closeable pdr = () -> reader.close();
            PrintWriter out = new PrintWriter(new FileWriter(outFile))) {
            for (int i = 1, size = reader.getNumberOfPages(); i <= size; i++) {
                parser.processContent(i, new MyTextExtractionStrategy(out, i));
            }
        }
    }

    @Test
    public void testExamineTuti() throws IOException {
        examine("data/tuti.pdf", "data/tuti-itext-examine.txt");
    }

    @Test
    public void testExamineKokuji() throws IOException {
        examine("data/kokuji.pdf", "data/kokuji-itext-examine.txt");
    }

}
