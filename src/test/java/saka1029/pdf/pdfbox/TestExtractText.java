package saka1029.pdf.pdfbox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;

public class TestExtractText {
	
	static {
	    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
	}

	static final PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

	static class Stripper extends PDFTextStripper {

		public Stripper() throws IOException {
			super();
		}
		
		// これをオーバーライドすると文字単位に位置、フォント、フォントサイズなどの
        // 情報を処理できる。
//		@Override
//		protected void processTextPosition(TextPosition text) {
//			out.println(text);
//		}
		
	}
	static void extractText(File pdfFile, File textFile) throws IOException {
	    try (PDDocument doc = PDDocument.load(pdfFile);
				FileWriter w = new FileWriter(textFile);
				PrintWriter p = new PrintWriter(w)) {
	    	Stripper stripper = new Stripper();
	    	stripper.setSortByPosition(true);
//	    	stripper.setIndentThreshold(0.0F);
			String text = stripper.getText(doc);
			p.print(text);
	    }
	}

	@Test
	public void testExtractText() throws IOException {
		extractText(new File("data/tuti.pdf"), new File("data/pdfbox-tuti.txt"));
		extractText(new File("data/kokuji.pdf"), new File("data/pdfbox-kokuji.txt"));
	}

}
