package saka1029.pdf.itext;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Test;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class TestStatistics {

    static final PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    // A4のポイントサイズ
    // 横約8.27 × 縦約11.69 インチ
    // 595.44 x 841.68 ポイント
    static final int PAGE_WIDTH = 596, PAGE_HEIGHT = 842;
//    static final Pattern HIRAGANA = Pattern.compile("\\p{IsHiragana}+");
//
//    static boolean isHiragana(String s) {
//    	return HIRAGANA.matcher(s).matches();
//    }

    static class Text implements Comparable<Text> {
    	final int x, y, width, extra;
    	final String text;
    	
    	static int round(float f) {
    		return Math.round(f);
    	}

    	Text(int x, int y, int width, String text, int extra) {
    	    this.x = x;
    	    this.y = y;
    	    this.width = width;
    	    this.extra = extra;
    	    this.text = text;
    	}

    	Text(Text text, int extra) {
    	    this(text.x, text.y, text.width, text.text, extra);
    	}

    	static Rectangle2D.Float box(TextRenderInfo info) {
            return info.getAscentLine().getBoundingRectange();
    	}
    	
    	Text(TextRenderInfo info, boolean horizontal) {
    	    this(round(horizontal ? box(info).x : PAGE_HEIGHT - box(info).y),
    	        round(horizontal ? PAGE_HEIGHT - box(info).y : PAGE_WIDTH - box(info).x),
    	        round(box(info).width),
    	        info.getText(), 0);
    	}

		@Override
		public int compareTo(Text o) {
			int r = Integer.compare(x, o.x);
			if (r == 0)
			    r = Integer.compare(extra, o.extra);
			return r;
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
			
			String textString() {
				float halfWidth = Page.this.maxWidth / 2.0F;
				int position = 0;
				StringBuilder sb = new StringBuilder();
				for (Text text : texts) {
				    int spaces = (int)((text.x - position) / halfWidth);
				    if (spaces > 0)
                        sb.append(" ".repeat(spaces));
				    sb.append(text.text);
				    position = text.x + text.width;
				}
				return sb.toString();
			}
			
			boolean isSmall() {
			    return maxWidth <= Page.this.maxWidth * 0.6;
			}
			
			@Override
			public String toString() {
				String size = isSmall() ? "小" : "大";
				return "%02d(%s)%s".formatted(maxWidth, size, textString());
//				return "%02d(%s)%s".formatted(maxWidth, size, texts);
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
        
        static boolean ignore(Line text) {
            return text.textString().matches(" *- *\\d+ *- *");
        }

        static boolean ignore(Text text) {
            return text.text.matches("\\p{IsHiragana}+");
        }

        Page(PdfReaderContentParser parser, int pageNo, boolean horizontal) throws IOException {
        	this.pageNo = pageNo;
        	this.horizontal = horizontal;
        	// PDFパーサを使ってテキストを取り出す。（コールバック）
        	parser.processContent(pageNo, new Listener());
        	this.maxWidth = lines.values().stream()
        		.mapToInt(line -> line.maxWidth)
        		.max().getAsInt();
        	// 小文字の行を大文字の行にマージする。
        	List<Entry<Integer, Line>> smalls = new ArrayList<>();     // すべての小文字行
        	List<Entry<Integer, Line>> smallTemp = new ArrayList<>();  // 未処理の小文字行
        	Entry<Integer, Line> big = null;                           // 大文字行
        	for (Entry<Integer, Line> line : lines.entrySet()) {
        	    if (line.getValue().isSmall()) {
        	        smalls.add(line);
        	        smallTemp.add(line);
        	    } else {  // 大文字行の場合、未処理の小文字行をマージする。
                    for (Entry<Integer, Line> s : smallTemp) {
                        if (ignore(s.getValue())) // ページ番号の場合追加しない。
                            continue;
                        int sp = s.getKey();
                        // 最も近い大文字行を特定する。
                        Line nearBig = big == null ? line.getValue()
                            : Math.abs(big.getKey() - sp) < Math.abs(line.getKey() - sp) ? big.getValue()
                            : line.getValue();
                        for (Text t : s.getValue().texts)
                            if (!ignore(t))    // ルビの場合は追加しない。
//                            if (!t.text.matches("\\p{IsHiragana}+"))    // ルビの場合は追加しない。
                                nearBig.add(new Text(t, -t.y));
                    }
                    smallTemp.clear();
        	        big = line;
        	    }
        	}
        	if (big != null)
                for (Entry<Integer, Line> s : smallTemp) {
                    if (ignore(s.getValue())) // ページ番号の場合追加しない。
                        continue;
                    for (Text t : s.getValue().texts)
                        if (!ignore(t))    // ルビの場合は追加しない。
                            big.getValue().add(new Text(t, -t.y));
                }
        	for (Entry<Integer, Line> line : smalls)
        	    lines.remove(line.getKey());
        }
        
        @Override
        public String toString() {
        	StringBuilder sb = new StringBuilder();
            sb.append("page max width=" + maxWidth)
                .append(" page=" + pageNo)
                .append(System.lineSeparator());
        	for (Entry<Integer, Line> e : lines.entrySet())
				sb.append("%03d@%03d:%s%n".formatted(pageNo, e.getKey(), e.getValue()));
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
