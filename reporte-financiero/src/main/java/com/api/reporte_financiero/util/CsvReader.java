package com.api.reporte_financiero.util;

import com.api.reporte_financiero.model.Transaction;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvReader {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("d/M/yyyy");

    public List<Transaction> read(MultipartFile file) {

        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {

                // Omitir encabezado
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // 🔹 LIMPIAR COMILLAS
                String cleanLine = line.replace("\"", "").trim();

                // Ignorar líneas vacías
                if (cleanLine.isEmpty()) continue;

                String[] data = cleanLine.split(";");

                if (data.length != 4) {
                    System.out.println("Línea inválida: " + line);
                    continue;
                }

                try {
                    Transaction tx = new Transaction(
                            data[0].trim(),
                            data[1].trim(),
                            Double.parseDouble(data[2].trim()),
                            LocalDate.parse(data[3].trim(), FORMATTER)
                    );
                    transactions.add(tx);

                } catch (Exception e) {
                    System.out.println(
                            "Error en línea: " + line + " → " + e.getMessage()
                    );
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error leyendo CSV: " + e.getMessage(), e);
        }

        System.out.println("Transacciones leídas: " + transactions.size());
        return transactions;
    }
}
