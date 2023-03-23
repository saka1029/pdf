package saka1029.pdf.itext;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
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
//			return text;
		}
    }

    static class Page {
        final NavigableMap<Integer, NavigableSet<Text>> texts = new TreeMap<>();
        final boolean horizontal;
        final Map<Integer, Integer> widthHistogram = new HashMap<>();
        
        Page(PdfReaderContentParser parser, int i, boolean horizontal) throws IOException {
        	this.horizontal = horizontal;
        	parser.processContent(i, new TextExtractionStrategy() {
				@Override
				public void renderText(TextRenderInfo renderInfo) {
		        	if (renderInfo.getText().isBlank())
		        		return;
					Text t = new Text(renderInfo, horizontal);
					if (!t.text.isBlank())
						widthHistogram.compute(t.width / t.text.length(), (k, v) -> v == null ? 1 : v + 1);
					texts.computeIfAbsent(t.y, k -> new TreeSet<>()).add(t);
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
        	});
        }

        
        int mostFrequentryWidth() {
        	Entry<Integer, Integer> max = Map.entry(0, 0);
        	for (Entry<Integer, Integer> e : widthHistogram.entrySet())
				if (e.getValue() > max.getValue())
					max = e;
        	return max.getKey();
        }
    }

    static void examine(String pdfFile, String outFile, boolean horizontal) throws IOException {
        PdfReader reader = new PdfReader(pdfFile);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
//        List<NavigableMap<Integer, NavigableSet<Text>>> doc = new ArrayList<>();
        try (Closeable pdr = () -> reader.close();
            PrintWriter out = new PrintWriter(new FileWriter(outFile))) {
            for (int i = 1, size = reader.getNumberOfPages(); i <= size; i++) {
            	Page page = new Page(parser, i, horizontal);
                int mostFrequentryWidth = page.mostFrequentryWidth();
                out.println("width histogram: " + page.widthHistogram + " most freq width=" + mostFrequentryWidth);
                int halfSpaceWidth = mostFrequentryWidth / 2;
                for (Entry<Integer, NavigableSet<Text>> e : page.texts.entrySet())
                	out.println(i + "@" + e.getKey() + ":"
                	+ " ".repeat(Math.abs(e.getValue().iterator().next().x) / halfSpaceWidth)
                	+ e.getValue().stream().map(x -> x.text).collect(Collectors.joining()));
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
}
