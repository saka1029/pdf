package saka1029.pdf.itext;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class TestStatistics {

    static final PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    static final int PAGE_WIDTH = 600, PAGE_HEIGHT = 850;
    static final Pattern HIRAGANA = Pattern.compile("\\p{IsHiragana}+");

    static boolean isHiragana(String s) {
    	return HIRAGANA.matcher(s).matches();
    }

    static class Text implements Comparable<Text> {
    	final int x, y, width;
    	final String text;
    	
    	static int round(float f) {
    		return Math.round(f);
    	}

    	Text(TextRenderInfo info, boolean horizontal) {
            Rectangle2D.Float box = info.getAscentLine().getBoundingRectange();
			this.x = round(horizontal ? box.x : PAGE_HEIGHT - box.y);
			this.y = round(horizontal ? PAGE_HEIGHT - box.y : PAGE_WIDTH - box.x);
            this.width = round(box.width);
            this.text = info.getText();
    	}

		@Override
		public int compareTo(Text o) {
			return Integer.compare(x, o.x);
		}
		
		@Override
		public String toString() {
			return "%dx%d:%d:%s".formatted(x, y, width, text);
		}
    }

    static class Page {
    	final int pageNo;
        final NavigableMap<Integer, Line> lines = new TreeMap<>();
        final boolean horizontal;
        final int maxWidth;
        
		class Line {
			final NavigableSet<Text> texts = new TreeSet<>();
			int maxWidth = 2;
			
			void add(Text text) {
				texts.add(text);
				if (!text.text.isBlank())
					maxWidth = Math.max(maxWidth, text.width / text.text.length());
			}
			
			@Override
			public String toString() {
				int pageMaxWidth = Page.this.maxWidth;
				return "max width=" + maxWidth + " ".repeat(Math.abs(texts.iterator().next().x) / (pageMaxWidth / 2))
					+ texts.stream().map(t -> t.text).collect(Collectors.joining());
			}
		}

        class Listener implements TextExtractionStrategy {

			@Override
			public void renderText(TextRenderInfo renderInfo) {
				if (renderInfo.getText().isBlank())
					return;
				Text t = new Text(renderInfo, horizontal);
				lines.computeIfAbsent(t.y, k -> new Line()).add(t);
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

        Page(PdfReaderContentParser parser, int pageNo, boolean horizontal) throws IOException {
        	this.pageNo = pageNo;
        	this.horizontal = horizontal;
        	parser.processContent(pageNo, new Listener());
        	this.maxWidth = lines.values().stream()
        		.mapToInt(line -> line.maxWidth)
        		.max().getAsInt();
        }
        
        @Override
        public String toString() {
        	StringBuilder sb = new StringBuilder();
            sb.append("page max width=" +maxWidth).append(System.lineSeparator());
        	for (Entry<Integer, Line> e : lines.entrySet())
				sb.append(pageNo + "@" + e.getKey() + ":" + e.getValue() + System.lineSeparator());
        	return sb.toString();
        }
    }

    static void examine(String pdfFile, String outFile, boolean horizontal) throws IOException {
        PdfReader reader = new PdfReader(pdfFile);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        try (Closeable pdr = () -> reader.close();
            PrintWriter out = new PrintWriter(new FileWriter(outFile))) {
            for (int i = 1, size = reader.getNumberOfPages(); i <= size; i++) {
            	Page page = new Page(parser, i, horizontal);
            	out.print(page);
            }
        }
    }

    @Test
    public void testExamineTuti() throws IOException {
        examine("data/tuti.pdf", "data/tuti-itext-statistics.txt", true);
    }

    @Test
    public void testExamineKokuji() throws IOException {
        examine("data/kokuji.pdf", "data/kokuji-itext-statistics.txt", false);
    }
    
    @Test
    public void testHiragana() {
        out.println("あゐいうぁぃか".matches("\\p{IsHiragana}+"));
    }
}
