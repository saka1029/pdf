package saka1029.pdf;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.IntStream;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

/**
 * PDFファイルからテキストを抽出します。
 * 縦書きのPDFに対応しています。
 * ルビは無条件に削除します。
 * 内部座標値は整数のポイントで表現します。
 * <br>
 * ＜使用例＞
 * <pre><code>
 * // インスタンスを作成します。
 * // 縦書きの時は第2引数をfalseにします。
 * ITextPdf pdf = new ITextPdf("tuti.pdf", true);
 * // ここで必要に応じてフィールドを変更します。
 * pdf.newline = "\r\n";
 * pdf.pagePattern = " *\\(\\d+\\) *";
 * // ファイルを読み込みます。
 * pdf.parse();
 * // テキストを取り出します。
 * String text = pdf.text();
 * </code></pre>
 */
public class ITextPdf {
	// A4のポイントサイズ
	// 横約8.27 × 縦約11.69 インチ
	// 595.44 x 841.68 ポイント
	static final int PAGE_WIDTH = 596, PAGE_HEIGHT = 842;
	/**
	 * 医科通知のページ番号は
	 * 平成30年度「初・再診  - 1 -」
	 * 令和01年度「初・再診  - 1 -」
	 * 令和02年度「- 1 -」
	 * 令和04年度「- 1 -」
	 */
	static final String DEFAULT_PAGE_PATTERN = "\\s*\\S*\\s*-\\s*\\d+\\s*-\\s*";
	static final String DEFAULT_NEWLINE = "\r\n";
	static final float DEFAULT_LINE_HEIGHT_RATE = 1.0F;

	public final String filename;
	public final boolean horizontal;
	public String pagePattern = DEFAULT_PAGE_PATTERN;
	public String newline = DEFAULT_NEWLINE;
	public float lineHeightRate = DEFAULT_LINE_HEIGHT_RATE;
	public int numberOfPages;
	public int minX = Integer.MAX_VALUE;
	final List<Page> pages = new ArrayList<>();

	public ITextPdf(String filename, boolean horizontal) throws IOException {
		this.filename = filename;
		this.horizontal = horizontal;
	}
	
	public void parse() throws IOException {
		PdfReader reader = new PdfReader(filename);
		try (Closeable c = () -> reader.close()) {
			this.numberOfPages = reader.getNumberOfPages();
			PdfReaderContentParser parser = new PdfReaderContentParser(reader);
			for (int i = 1; i <= numberOfPages; ++i)
				pages.add(new Page(parser, i));
		}
	}

	public String text() {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= numberOfPages; ++i) {
			Page page = pages.get(i - 1);
			sb.append("# %s page: %d%s".formatted(filename, i, newline));
			for (Page.Line line : page.lines.values())
				sb.append(line.text()).append(newline);
		}
		return sb.toString();
	}
	
	public static void toText(String[] inFiles, String outFile, boolean horizontal) throws IOException {
		try (Writer out = new FileWriter(outFile)) {
			for (String inFile : inFiles) {
				ITextPdf pdf = new ITextPdf(inFile, horizontal);
				pdf.parse();
				out.write(pdf.text());
			}
		}
	}

	class Page {
		final int pageNo;
		final NavigableMap<Integer, Line> lines = new TreeMap<>();
		int maxWidth = Integer.MAX_VALUE;

		Page(PdfReaderContentParser parser, int pageNo) throws IOException {
			this.pageNo = pageNo;
			// PDFパーサーを使ってテキストを取り出す。（コールバック）
			parser.processContent(pageNo, new PageListener());
			for (Iterator<Entry<Integer, Line>> it = lines.entrySet().iterator(); it.hasNext();) {
				Line line = it.next().getValue();
				if (line.text().matches(pagePattern))
					it.remove();
				else
					this.maxWidth = Math.max(this.maxWidth, line.maxWidth);
			}
			this.maxWidth = lines.values().stream().mapToInt(line -> line.maxWidth).max().orElse(0);
			// 小文字行を大文字行にマージします。
			mergeLines();
		}
		
		void mergeLines() {
			float height = maxWidth * lineHeightRate;
			Iterator<Entry<Integer, Line>> iterator = lines.entrySet().iterator();
			if (!iterator.hasNext())
				return;
			Entry<Integer, Line> first = iterator.next();
			while (iterator.hasNext()) {
				Line firstLine = first.getValue();
				float limit = first.getKey() + height;
				Entry<Integer, Line> next = null;
				while (iterator.hasNext() && (next = iterator.next()).getKey() <= limit) {
					// next行をfirst行にマージします。
					Line nextLine = next.getValue();
					for (Text text : nextLine.texts)
						if (!(nextLine.isSmall() && text.isHiragana()))    // ルビの場合は追加しません。
							firstLine.add(new Text(text, -text.y));
					iterator.remove();
				}
				first = next;
			}
		}

		/**
		 * 小文字行群を最も近い大文字行にマージします。
		 * 
		 * @param big1 小文字行群の前にある大文字行です。null可
		 * @param big2 小文字行群の後にある大文字行です。null可
		 * @param smallTemp マージすべき小文字行のリストです。
		 */
		void mergeLines(Entry<Integer, Line> big1, Entry<Integer, Line> big2, List<Entry<Integer, Line>> smallTemp) {
		    if (big1 == null && big2 == null)
		        return;
		    for (Entry<Integer, Line> s : smallTemp) {
//		        if (s.getValue().text().matches(pagePattern)) // ページ番号の場合追加しない。
//		            continue;
                int sp = s.getKey();
                // 最も近い大文字行を特定する。
                Line nearBig = big1 == null ? big2.getValue()
                    : big2 == null ? big1.getValue()
                    : Math.abs(big1.getKey() - sp) < Math.abs(big2.getKey() - sp) ? big1.getValue()
                    : big2.getValue();
                for (Text t : s.getValue().texts)
                    if (!t.isHiragana())    // ルビの場合は追加しない。
                        nearBig.add(new Text(t, -t.y));
		    }
		}
        
		/**
		 * 小文字の行を大文字の行にマージする。
		 */
//		void mergeLines() {
//        	List<Entry<Integer, Line>> smalls = new ArrayList<>();     // すべての小文字行
//        	List<Entry<Integer, Line>> smallTemp = new ArrayList<>();  // 未処理の小文字行
//        	Entry<Integer, Line> big = null;                           // 大文字行
//        	for (Entry<Integer, Line> line : lines.entrySet()) {
//        	    if (line.getValue().isSmall()) {
//        	        smalls.add(line);
//        	        smallTemp.add(line);
//        	    } else {  // 大文字行の場合、未処理の小文字行をマージする。
//        	        mergeLines(big, line, smallTemp);
//                    smallTemp.clear();
//        	        big = line;
//        	    }
//        	}
//        	mergeLines(big, null, smallTemp);
//        	// マージしたすべての小文字行を削除する。
//        	for (Entry<Integer, Line> line : smalls)
//        	    lines.remove(line.getKey());
//		}

		class PageListener implements TextExtractionStrategy {

			@Override
			public void renderText(TextRenderInfo renderInfo) {
				if (renderInfo.getText().isBlank())
					return;
				Text t = Page.this.new Text(renderInfo);
				lines.computeIfAbsent(t.y, k -> new Line()).add(t);
				minX = Math.min(minX, t.x);
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

		class Line {
			int maxWidth = 2;
			final NavigableSet<Text> texts = new TreeSet<>();

			void add(Text text) {
				texts.add(text);
				if (text.text.isBlank())
					return;
				int width = text.width /text.text.length();
				maxWidth = Math.max(maxWidth, width);
			}
			
			boolean isSmall() {
				return maxWidth <= Page.this.maxWidth * 0.6;
			}
			
			/**
			 * テキストを連結して１行分の文字列を返します。
			 * テキストの間隔により、適宜半角スペースをは挿入します。
			 */
			String text() {
				float halfWidth = Page.this.maxWidth / 2.0F;
				int position = minX;
				StringBuilder sb = new StringBuilder();
				for (Text text : texts) {
				    int spaces = (int)((text.x - position) / halfWidth);
				    for (int i = 0; i < spaces; ++i)
                        sb.append(" ");
				    sb.append(text.text);
				    position = text.x + text.width;
				}
				return sb.toString();
			}
		}

		class Text implements Comparable<Text> {
			final int x, y, width, extra;
			final String text;

			static int round(float f) {
				return Math.round(f);
			}

			Text(int x, int y, int width, String text, int extra) {
				this.x = x;
				this.y = y;
				this.width = width;
				this.text = text;
				this.extra = extra;
			}

			Text(Text text, int extra) {
				this(text.x, text.y, text.width, text.text, extra);
			}

			static Rectangle2D.Float box(TextRenderInfo info) {
				return info.getAscentLine().getBoundingRectange();
			}

			Text(TextRenderInfo info) {
				this(round(horizontal ? box(info).x : PAGE_HEIGHT - box(info).y),
						round(horizontal ? PAGE_HEIGHT - box(info).y : PAGE_WIDTH - box(info).x),
						round(box(info).width), info.getText(), 0);
			}

			boolean isHiragana() {
				return text.matches("\\p{IsHiragana}+");
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
	}

}
