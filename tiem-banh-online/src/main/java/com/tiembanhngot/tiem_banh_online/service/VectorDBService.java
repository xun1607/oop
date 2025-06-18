package com.tiembanhngot.tiem_banh_online.service;

import org.springframework.stereotype.Service;
import java.util.Scanner;


@Service
public class VectorDBService {

    public String searchRelevantContext(String query) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                "python", "D:\\project\\tiem-banh-online\\tiem-banh-online\\vectorize\\search.py", query
            );
            builder.redirectErrorStream(true);
            Process process = builder.start();

            Scanner scanner = new Scanner(process.getInputStream());
            StringBuilder result = new StringBuilder();
            while (scanner.hasNextLine()) {
                result.append(scanner.nextLine()).append("\n");
            }
            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
