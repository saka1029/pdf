package saka1029.pdf;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class IText {

    public static List<List<Text>> 読み込み(String filename) throws IOException {
        List<List<Text>> result = new ArrayList<>();
        PdfReader reader = new PdfReader(filename);
        try (Closeable c = () -> reader.close()) {
            int numberOfPages = reader.getNumberOfPages();
            PdfReaderContentParser parser = new PdfReaderContentParser(reader);
            for (int i = 1; i <= numberOfPages; ++i) {
                List<Text> page = new ArrayList<>();
                result.add(page);
                parser.processContent(i, new RenderListener() {
                    public void renderText(TextRenderInfo info) {
                        Rectangle2D.Float baseBox = info.getBaseline().getBoundingRectange();
                        float ascent = info.getAscentLine().getBoundingRectange().y;
                        float descent = info.getDescentLine().getBoundingRectange().y;
                        page.add(new Text(baseBox.x, baseBox.y, baseBox.width, ascent - descent, info.getText()));
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
        }
        return result;
    }

}
