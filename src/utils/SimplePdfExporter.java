package utils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports a Swing component (e.g. a report panel) as a single-page PDF.
 * <p>
 * No external library (iText / PDFBox / etc.) is required: the component is
 * rendered to a JPEG image in memory, and that image is wrapped inside a
 * hand-written, minimal-but-valid PDF file (one page, one embedded image).
 */
public final class SimplePdfExporter {

    /** A4 landscape, in PDF points (72 pt = 1 inch). Good fit for wide dashboard panels. */
    private static final double PAGE_WIDTH = 842;
    private static final double PAGE_HEIGHT = 595;
    private static final double MARGIN = 24;

    private SimplePdfExporter() {
    }

    /**
     * Renders {@code component} and writes it as a one-page PDF to {@code outputFile}.
     */
    public static void exportComponentAsPdf(Component component, File outputFile) throws IOException {
        BufferedImage image = renderComponentToImage(component);
        byte[] jpegBytes = toJpegBytes(image);
        writePdf(jpegBytes, image.getWidth(), image.getHeight(), outputFile);
    }

    // ==========================================
    // Step 1: render the component to a raster image
    // ==========================================
    private static BufferedImage renderComponentToImage(Component component) {
        int width = Math.max(1, component.getWidth());
        int height = Math.max(1, component.getHeight());

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        boolean wasDoubleBuffered = component.isDoubleBuffered() ? true : false;
        if (component instanceof javax.swing.JComponent) {
            ((javax.swing.JComponent) component).setDoubleBuffered(false);
        }
        component.print(g2d);
        if (component instanceof javax.swing.JComponent) {
            ((javax.swing.JComponent) component).setDoubleBuffered(wasDoubleBuffered);
        }

        g2d.dispose();
        return image;
    }

    private static byte[] toJpegBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    // ==========================================
    // Step 2: hand-write a minimal valid single-page PDF embedding the JPEG
    // ==========================================
    private static void writePdf(byte[] jpegBytes, int pixelWidth, int pixelHeight, File outputFile) throws IOException {
        // Fit the image inside the page (minus margins) while preserving aspect ratio, centered.
        double maxW = PAGE_WIDTH - 2 * MARGIN;
        double maxH = PAGE_HEIGHT - 2 * MARGIN;
        double scale = Math.min(maxW / pixelWidth, maxH / pixelHeight);
        double drawW = pixelWidth * scale;
        double drawH = pixelHeight * scale;
        double x = (PAGE_WIDTH - drawW) / 2.0;
        double y = (PAGE_HEIGHT - drawH) / 2.0;

        String contentStream = String.format(
                "q %.2f 0 0 %.2f %.2f %.2f cm /Im0 Do Q",
                drawW, drawH, x, y);
        byte[] contentBytes = contentStream.getBytes(StandardCharsets.US_ASCII);

        List<Long> offsets = new ArrayList<>();
        try (FileOutputStream rawOut = new FileOutputStream(outputFile)) {
            CountingOutputStream out = new CountingOutputStream(rawOut);

            writeAscii(out, "%PDF-1.4\n");

            // Object 1: Catalog
            offsets.add(out.count);
            writeAscii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            // Object 2: Pages
            offsets.add(out.count);
            writeAscii(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            // Object 3: Page
            offsets.add(out.count);
            writeAscii(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R "
                    + "/MediaBox [0 0 " + fmt(PAGE_WIDTH) + " " + fmt(PAGE_HEIGHT) + "] "
                    + "/Resources << /XObject << /Im0 5 0 R >> >> "
                    + "/Contents 4 0 R >>\nendobj\n");

            // Object 4: Content stream
            offsets.add(out.count);
            writeAscii(out, "4 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
            out.write(contentBytes);
            writeAscii(out, "\nendstream\nendobj\n");

            // Object 5: Image XObject (raw JPEG via DCTDecode)
            offsets.add(out.count);
            writeAscii(out, "5 0 obj\n<< /Type /XObject /Subtype /Image "
                    + "/Width " + pixelWidth + " /Height " + pixelHeight + " "
                    + "/ColorSpace /DeviceRGB /BitsPerComponent 8 "
                    + "/Filter /DCTDecode /Length " + jpegBytes.length + " >>\nstream\n");
            out.write(jpegBytes);
            writeAscii(out, "\nendstream\nendobj\n");

            long xrefStart = out.count;
            int objectCount = offsets.size() + 1; // +1 for object 0 (free)
            writeAscii(out, "xref\n0 " + objectCount + "\n");
            writeAscii(out, "0000000000 65535 f \n");
            for (long offset : offsets) {
                writeAscii(out, String.format("%010d 00000 n \n", offset));
            }
            writeAscii(out, "trailer\n<< /Size " + objectCount + " /Root 1 0 R >>\n");
            writeAscii(out, "startxref\n" + xrefStart + "\n%%EOF");

            out.flush();
        }
    }

    private static String fmt(double v) {
        if (v == Math.rint(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    private static void writeAscii(OutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.US_ASCII));
    }

    /** Tiny wrapper that tracks how many bytes have been written so far (for the xref table). */
    private static final class CountingOutputStream extends OutputStream {
        private final OutputStream delegate;
        long count = 0;

        CountingOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            count += len;
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}