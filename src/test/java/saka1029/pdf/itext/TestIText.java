package saka1029.pdf.itext;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;

import saka1029.pdf.IText;

public class TestIText {

	static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

	/**
	 * デバッグ出力するファイル名、ページ番号、行番号を指定します。
	 */
	static final Map<String, Map<Integer, Set<Integer>>> DEBUG_MAP = new HashMap<>();

	static {
//	    DEBUG_MAP.put(ファイル名, Map.of(ページ番号, Set.of(行番号...)));
//	    DEBUG_MAP.put("data/kokuji.pdf", Map.of(22, Set.of(8, 9, 10, 11)));
	    DEBUG_MAP.put("data/tuti.pdf", Map.of(1, Set.of(1, 2, 3, 4, 5, 6, 7, 8)));
	}
	
	/**
	 * DEBUG_MAPで指定した条件に合致する行(Elementの集合)を出力します。
	 */
	static final IText.DebugElement DEBUG_ELEMENT = (path, pageNo, lineNo, attr, elements) -> {
	    Map<Integer, Set<Integer>> pageLine = DEBUG_MAP.get(path);
	    if (pageLine != null) {
            Set<Integer> lines = pageLine.get(pageNo);
            if (lines != null && lines.contains(lineNo))
                OUT.printf("%s:%d:%d:%s%n", path, pageNo, lineNo, elements);
	    }
	};
	
	static void read(boolean horizontal, String out, String... ins) throws IOException {
	    IText itext = new IText(horizontal);
	    itext.debugElement = DEBUG_ELEMENT;
	    itext.テキスト変換(out, ins);
	}

	@Test
	public void testRead() throws IOException {
		read(true, "data/tuti-itext.txt", "tuti.pdf");
		read(false, "data/kokuji-itext.txt", "kokuji.pdf");
	}
	
//	@Test
	public void testMatcher() {
		Pattern pat = Pattern.compile("^\\s*\\S*\\s*-\\s*\\d+\\s*-\\s*$");
		OUT.println(pat.matcher("  - 3 -").replaceFirst("#$0"));
		OUT.println(pat.matcher("  -3-").replaceFirst("#$0"));
		OUT.println(pat.matcher("  - 231 -").replaceFirst("#$0"));
		OUT.println(pat.matcher(" 加算  - 3 -").replaceFirst("#$0"));
		OUT.println(pat.matcher(" - -").replaceFirst("#$0"));
	}
	
	static void write(PdfReader reader, String outFile, int start, int end) throws DocumentException, IOException {
//		Document document = new Document(reader.getPageSizeWithRotation(1));
		Document document = new Document();
		PdfCopy writer = new PdfCopy(document, new FileOutputStream(outFile));
		try (Closeable w = () -> writer.close()) {
			document.open();
			try (Closeable d = () -> document.close()) {
				for (int i = start; i <= end; ++i) {
					PdfImportedPage page = writer.getImportedPage(reader, i);
					writer.addPage(page);
				}
			}
		}
	}

	@Test
	public void testSplit() throws IOException, DocumentException {
		String inFile = "0000196315.pdf";
		String outFile12 = "0000196315-1-2.pdf";
		String outFile34 = "0000196315-379-380.pdf";
		PdfReader reader = new PdfReader(inFile);
		try (Closeable r = () -> reader.close()) {
			int n = reader.getNumberOfPages();
			OUT.println("ページ数: " + n);
			write(reader, outFile12, 1, 2);
			write(reader, outFile34, 379, 380);
		}
	}
}
