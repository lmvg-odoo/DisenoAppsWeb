package com.api.reporte_financiero.util;

import com.api.reporte_financiero.model.Transaction;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class PdfGenerator {

    public byte[] generate(
            List<Transaction> incomes,
            List<Transaction> selfTransfers,
            List<Transaction> allTransactions,
            String username
    ) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(document, page);
            cs.setLeading(14.5f);
            cs.beginText();
            cs.newLineAtOffset(40, 750);

            // Usuario
            cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
            cs.showText("Usuario autenticado: " + username + " - acceso correcto");
            cs.newLine();
            cs.newLine();

            // Título
            cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
            cs.showText("Reporte de Transacciones Financieras");
            cs.newLine();
            cs.newLine();

            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.showText("Resumen general de movimientos registrados");
            cs.newLine();
            cs.newLine();

            // Ingresos
            cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
            cs.showText("Ingresos Nuevos");
            cs.newLine();

            cs.setFont(PDType1Font.HELVETICA, 12);
            for (Transaction tx : incomes) {
                cs.showText(tx.getDate() + " - " + tx.getAccountFrom()
                        + " -> " + tx.getAccountTo() + " : $" + tx.getAmount());
                cs.newLine();
            }

            cs.newLine();

            // Autotransferencias
            cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
            cs.showText("Autotransferencias");
            cs.newLine();

            cs.setFont(PDType1Font.HELVETICA, 12);
            for (Transaction tx : selfTransfers) {
                cs.showText(tx.getDate() + " - " + tx.getAccountFrom()
                        + " -> " + tx.getAccountTo() + " : $" + tx.getAmount());
                cs.newLine();
            }

            cs.endText();

            // =========================
            // TABLA HISTORIAL
            // =========================

            float startY = 520;
            float startX = 40;
            float rowHeight = 20;

            // 🔹 5 columnas
            float[] colWidths = {90, 120, 120, 80, 120};

            // Encabezados
            PDColor headerBg =
                    new PDColor(new float[]{0.2f, 0.4f, 0.6f}, PDDeviceRGB.INSTANCE);

            drawHeaderRow(
                    cs,
                    startX,
                    startY,
                    rowHeight,
                    colWidths,
                    new String[]{"Fecha", "Origen", "Destino", "Monto", "Categoría"},
                    headerBg
            );

            float y = startY - rowHeight;
            boolean alternate = false;

            for (Transaction tx : allTransactions) {

                PDColor rowBg = alternate
                        ? new PDColor(new float[]{0.95f, 0.95f, 0.95f}, PDDeviceRGB.INSTANCE)
                        : null;

                drawDataRow(
                        cs,
                        startX,
                        y,
                        rowHeight,
                        colWidths,
                        new String[]{
                                tx.getDate().toString(),
                                tx.getAccountFrom(),
                                tx.getAccountTo(),
                                "$" + tx.getAmount(),
                                getCategory(tx)
                        },
                        rowBg
                );

                y -= rowHeight;
                alternate = !alternate;

                if (y < 50) {
                    cs.close();
                    page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);
                    cs = new PDPageContentStream(document, page);
                    y = 750;
                }
            }

            cs.close();
            document.save(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    // =========================
    // CATEGORÍA
    // =========================
    private String getCategory(Transaction tx) {
        if (tx.getAccountFrom().equals(tx.getAccountTo())) {
            return "Autotransferencia";
        }
        return "Ingreso";
    }

    // =========================
    // ENCABEZADO
    // =========================
    private void drawHeaderRow(
            PDPageContentStream cs,
            float x,
            float y,
            float height,
            float[] widths,
            String[] texts,
            PDColor bgColor
    ) throws Exception {

        cs.setNonStrokingColor(bgColor);
        cs.addRect(x, y - height, sum(widths), height);
        cs.fill();

        cs.setNonStrokingColor(0, 0, 0);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        drawRow(cs, x, y, height, widths, texts);
    }

    // =========================
    // FILA DE DATOS
    // =========================
    private void drawDataRow(
            PDPageContentStream cs,
            float x,
            float y,
            float height,
            float[] widths,
            String[] texts,
            PDColor bgColor
    ) throws Exception {

        if (bgColor != null) {
            cs.setNonStrokingColor(bgColor);
            cs.addRect(x, y - height, sum(widths), height);
            cs.fill();
            cs.setNonStrokingColor(0, 0, 0);
        }

        cs.setFont(PDType1Font.HELVETICA, 11);
        drawRow(cs, x, y, height, widths, texts);
    }

    // =========================
    // FILA GENÉRICA
    // =========================
    private void drawRow(
            PDPageContentStream cs,
            float x,
            float y,
            float height,
            float[] widths,
            String[] texts
    ) throws Exception {

        float textX = x + 5;
        float textY = y - 15;

        cs.moveTo(x, y);
        cs.lineTo(x + sum(widths), y);
        cs.stroke();

        cs.moveTo(x, y - height);
        cs.lineTo(x + sum(widths), y - height);
        cs.stroke();

        float colX = x;

        for (int i = 0; i < widths.length; i++) {

            cs.moveTo(colX, y);
            cs.lineTo(colX, y - height);
            cs.stroke();

            cs.beginText();
            cs.newLineAtOffset(textX, textY);
            cs.showText(texts[i]);
            cs.endText();

            colX += widths[i];
            textX += widths[i];
        }

        cs.moveTo(colX, y);
        cs.lineTo(colX, y - height);
        cs.stroke();
    }

    private float sum(float[] arr) {
        float s = 0;
        for (float f : arr) s += f;
        return s;
    }
}
