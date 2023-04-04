package saka1029.pdf.itext;

import static org.junit.Assert.*;

import org.junit.Test;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class TestIText {

	public record Text(float x, float y, float width, float height, String text) {
	}
	
	public static class IText {
		/** A4のポイントサイズ
		 * 横約8.27 × 縦約11.69 インチ
		 * 595.44 x 841.68 ポイント
		 */
		static final float PAGE_WIDTH = 596F, PAGE_HEIGHT = 842F;
		
		Text text(TextRenderInfo info, boolean horizontal) {
			Rectangle2D.Float baseBox = info.getBaseline().getBoundingRectange();
			Rectangle2D.Float ascentBox = info.getAscentLine().getBoundingRectange();
			return new Text(
				horizontal ? baseBox.x : PAGE_HEIGHT - baseBox.y,
				horizontal ? PAGE_HEIGHT - baseBox.y : PAGE_HEIGHT - baseBox.x,
				baseBox.width,
				ascentBox.x
					)
				this(round(horizontal ? box(info).x : PAGE_HEIGHT - box(info).y),
						round(horizontal ? PAGE_HEIGHT - box(info).y : PAGE_WIDTH - box(info).x),
						round(box(info).width), info.getText());
			}

		
	}

    @Test
    public void test() {
        fail("Not yet implemented");
    }

}
