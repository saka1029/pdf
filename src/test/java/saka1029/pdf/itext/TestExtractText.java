package saka1029.pdf.itext;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

public class TestExtractText {

    static final PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

//    static void extractText(String pdfFile) throws Exception {
//        try (var document = new PdfDocument(new PdfReader(pdfFile))) {
//            var strategy = new SimpleTextExtractionStrategy();
//            for (int i = 1; i < document.getNumberOfPages(); i++) {
//                String text = PdfTextExtractor.getTextFromPage(document.getPage(i), strategy);
//                System.out.println(text);
//            }
//        }
//    }

    static void extractText(String pdfFile, String txtFile) throws IOException {
        PdfReader reader = new PdfReader(pdfFile);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        try (Closeable pdr = () -> reader.close();
            PrintWriter out = new PrintWriter(new FileOutputStream(txtFile))) {
            for (int i = 1, size = reader.getNumberOfPages(); i <= size; i++) {
                TextExtractionStrategy strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
                out.println(strategy.getResultantText());
            }
        }
    }

    @Test
    public void testExtractText() throws IOException {
        extractText("data/tuti.pdf", "data/itext-tuti.txt");
        extractText("data/kokuji.pdf", "data/itext-kokuji.txt");
    }
}
