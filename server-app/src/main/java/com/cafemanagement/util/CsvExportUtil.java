package com.cafemanagement.util;

import com.cafemanagement.model.Product;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExportUtil {
    private static CsvExportUtil instance;

    private CsvExportUtil() {}

    public static synchronized CsvExportUtil getInstance() {
        if (instance == null) {
            instance = new CsvExportUtil();
        }
        return instance;
    }

    public String exportProducts(List<Product> products, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.append("ID,Name,Price,Category\n");
            
            // Write data
            for (Product product : products) {
                writer.append(String.valueOf(product.getId()))
                      .append(",")
                      .append(product.getName())
                      .append(",")
                      .append(product.getPrice().toString())
                      .append(",")
                      .append(product.getCategory())
                      .append("\n");
            }
        }
        return filePath;
    }
}
