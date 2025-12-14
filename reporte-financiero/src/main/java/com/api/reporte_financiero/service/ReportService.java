package com.api.reporte_financiero.service;

import com.api.reporte_financiero.model.Transaction;
import com.api.reporte_financiero.util.CsvReader;
import com.api.reporte_financiero.util.PdfGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ReportService {

    private final CsvReader csvReader;
    private final TransactionClassifier classifier;
    private final PdfGenerator pdfGenerator;

    public ReportService(CsvReader csvReader,
                         TransactionClassifier classifier,
                         PdfGenerator pdfGenerator) {
        this.csvReader = csvReader;
        this.classifier = classifier;
        this.pdfGenerator = pdfGenerator;
    }

    public byte[] processFile(MultipartFile file) {
        try {
            // Leer CSV
            List<Transaction> transactions = csvReader.read(file);

            // Clasificar
            List<Transaction> incomes = classifier.filterIncomes(transactions);
            List<Transaction> selfTransfers = classifier.filterSelfTransfers(transactions);

            // Usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            // Generar PDF
            return pdfGenerator.generate(
                incomes,
                selfTransfers,
                transactions,
                username
        );
        

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al procesar archivo y generar reporte: " + e.getMessage(), e
            );
        }
    }
}
