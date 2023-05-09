package saka1029.pdf;

/**
 * 様式一覧をHTML出力するためのレコードです。
 * IText.ページ分割()で作成します。
 */
public record 様式(String name, String id, int startPage, int endPage, String title) {
}
