package com.cafemanagement.util;

import com.cafemanagement.model.Product;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CsvImportUtil {
    private static CsvImportUtil instance;

    private CsvImportUtil() {}

    public static synchronized CsvImportUtil getInstance() {
        if (instance == null) {
            instance = new CsvImportUtil();
        }
        return instance;
    }

    public List<Product> importProducts(String filePath) throws IOException {
        List<Product> products = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }
                String[] data = line.split(",");
                if (data.length >= 4) {
                    Product product = new Product();
                    // Skip ID if we're importing new products
                    product.setName(data[1].trim());
                    product.setPrice(new BigDecimal(data[2].trim()));
                    product.setCategory(data[3].trim());
                    products.add(product);
                }
            }
        }
        return products;
    }
}
