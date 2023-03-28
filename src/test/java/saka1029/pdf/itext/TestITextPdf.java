package saka1029.pdf.itext;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.junit.Test;

import saka1029.pdf.ITextPdf;

public class TestITextPdf {

	@Test
	public void testTuti() throws IOException {
		ITextPdf pdf = new ITextPdf("data/tuti.pdf", true);
		try (Writer writer = new FileWriter("data/tuti-itextpdf.txt")) {
			writer.write(pdf.text());
		}
	}

	@Test
	public void testKokuji() throws IOException {
		ITextPdf pdf = new ITextPdf("data/kokuji.pdf", false);
		try (Writer writer = new FileWriter("data/kokuji-itextpdf.txt")) {
			writer.write(pdf.text());
		}
	}

}
