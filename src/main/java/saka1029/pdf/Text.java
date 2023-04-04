package saka1029.pdf;

/**
 * PDF内のテキスト要素を格納するクラスです。
 * x, yはX座標値、Y座標値です。
 * 座標は左上原点でX座標は右方向、Y座標は下方向です。 w, *
 * hは幅(X方向の長さ)、高さ(Y方向の長さ)です。
 * textはテキスト要素の文字列です。 単位はポイントです。1ポイントは1/72インチです。
 */
public class Text {
    public final float x, y, w, h;
    public final String text;

    public Text(float x, float y, float w, float h, String text) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.text = text;
    }

    @Override
    public String toString() {
        return x + "x" + y + ":" + w + "x" + h + ":" + text;
    }
}