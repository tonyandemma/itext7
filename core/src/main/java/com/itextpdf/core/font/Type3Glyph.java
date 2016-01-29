package com.itextpdf.core.font;

import com.itextpdf.core.PdfException;
import com.itextpdf.io.image.Image;
import com.itextpdf.io.source.OutputStream;
import com.itextpdf.core.pdf.canvas.PdfCanvas;
import com.itextpdf.core.pdf.PdfDocument;
import com.itextpdf.core.pdf.PdfStream;
import com.itextpdf.core.pdf.xobject.PdfXObject;

/**
 * The content where Type3 glyphs are written to.
 */
public final class Type3Glyph extends PdfCanvas {

    private static final String d0Str = "d0\n";
    private static final String d1Str = "d1\n";
    private static final byte[] d0 = OutputStream.getIsoBytes(d0Str);
    private static final byte[] d1 = OutputStream.getIsoBytes(d1Str);

    private float wx;
    private float llx;
    private float lly;
    private float urx;
    private float ury;
    private boolean isColor = false;

    /**
     * Creates a Type3Glyph canvas with a new Content Stream.
     *
     * @param pdfDocument the document that this canvas is created for
     */
    Type3Glyph(PdfDocument pdfDocument, float wx, float llx, float lly, float urx, float ury, boolean isColor) {
        super(new PdfStream().makeIndirect(pdfDocument), null, pdfDocument);
        writeMetrics(wx, llx, lly, urx, ury, isColor);
    }

    /**
     * Creates a Type3Glyph canvas with a non-empty Content Stream.
     *
     * @param pdfStream {@code PdfStream} from existed document.
     */
    Type3Glyph(PdfStream pdfStream) {
        super(pdfStream, null, pdfStream.getDocument());
        if (pdfStream.getBytes() != null) {
            fillBBFromBytes(pdfStream.getBytes());
        }
    }

    public float getWx() {
        return wx;
    }

    public float getLlx() {
        return llx;
    }

    public float getLly() {
        return lly;
    }

    public float getUrx() {
        return urx;
    }

    public float getUry() {
        return ury;
    }

    /**
     * Indicates if the glyph color specified in the glyph description or not.
     *
     * @return whether the glyph color is specified in the glyph description or not
     */
    public boolean isColor() {
        return isColor;
    }

    /**
     * Writes the width and optionally the bounding box parameters for a glyph
     *
     * @param wx      the advance this character will have
     * @param llx     the X lower left corner of the glyph bounding box. If the <CODE>isColor</CODE> option is
     *                <CODE>true</CODE> the value is ignored
     * @param lly     the Y lower left corner of the glyph bounding box. If the <CODE>isColor</CODE> option is
     *                <CODE>true</CODE> the value is ignored
     * @param urx     the X upper right corner of the glyph bounding box. If the <CODE>isColor</CODE> option is
     *                <CODE>true</CODE> the value is ignored
     * @param ury     the Y upper right corner of the glyph bounding box. If the <CODE>isColor</CODE> option is
     *                <CODE>true</CODE> the value is ignored
     * @param isColor defines whether the glyph color is specified in the glyph description in the font.
     *                The consequence of value <CODE>true</CODE> is that the bounding box parameters are ignored.
     */
    private void writeMetrics(float wx, float llx, float lly, float urx, float ury, boolean isColor) {
        this.isColor = isColor;
        this.wx = wx;

        this.llx = llx;
        this.lly = lly;
        this.urx = urx;
        this.ury = ury;

        if (isColor) {
            contentStream.getOutputStream()
                    .writeFloat(wx)
                    .writeSpace()
                    .writeFloat(0)//wy
                    .writeSpace()
                    .writeBytes(d0);
        } else {
            contentStream.getOutputStream()
                    .writeFloat(wx)
                    .writeSpace()
                    .writeFloat(0)//wy
                    .writeSpace()
                    .writeFloat(llx)
                    .writeSpace()
                    .writeFloat(lly)
                    .writeSpace()
                    .writeFloat(urx)
                    .writeSpace()
                    .writeFloat(ury)
                    .writeSpace()
                    .writeBytes(d1);
        }
    }

    /**
     * Creates Image XObject from image and adds it to canvas. Performs additional checks to make
     * sure that we only add mask images to not colorized type 3 fonts.
     *
     * @param image       the {@code PdfImageXObject} object
     * @param a           an element of the transformation matrix
     * @param b           an element of the transformation matrix
     * @param c           an element of the transformation matrix
     * @param d           an element of the transformation matrix
     * @param e           an element of the transformation matrix
     * @param f           an element of the transformation matrix
     * @param inlineImage true if to add image as in-line.
     * @return created Image XObject or null in case of in-line image (asInline = true).
     */
    @Override
    public PdfXObject addImage(Image image, float a, float b, float c, float d, float e, float f, boolean inlineImage) {
        if (!isColor && (!image.isMask() || !(image.getBpc() == 1 || image.getBpc() > 0xff))) {
            throw new PdfException("not.colorized.typed3.fonts.only.accept.mask.images");
        }
        return super.addImage(image, a, b, c, d, e, f, inlineImage);
    }

    private void fillBBFromBytes(byte[] bytes) {
        String str = new String(bytes);
        int d0Pos = str.indexOf(d0Str);
        int d1Pos = str.indexOf(d1Str);
        if (d0Pos != -1) {
            isColor = true;
            String[] bbArray = str.substring(0, d0Pos - 1).split(" ");
            if (bbArray.length == 2)
                this.wx = Float.parseFloat(bbArray[0]);
        } else if (d1Pos != -1) {
            isColor = false;
            String[] bbArray = str.substring(0, d1Pos - 1).split(" ");
            if (bbArray.length == 6) {
                this.wx = Float.parseFloat(bbArray[0]);
                this.llx = Float.parseFloat(bbArray[2]);
                this.lly = Float.parseFloat(bbArray[3]);
                this.urx = Float.parseFloat(bbArray[4]);
                this.ury = Float.parseFloat(bbArray[5]);
            }
        }
    }
}
