package saka1029.pdf;

/**
 * PDF内のテキスト要素を格納するクラスです。
 * x, yはX座標値、Y座標値です。
 * 座標は左上原点でX座標は右方向、Y座標は下方向です。 w, *
 * hは幅(X方向の長さ)、高さ(Y方向の長さ)です。
 * textはテキスト要素の文字列です。 単位はポイントです。1ポイントは1/72インチです。
 */
public record Text(float x, float y, float w, float h, String text) {
    @Override
    public String toString() {
        return x + "x" + y + ":" + w + "x" + h + ":" + text;
    }
}