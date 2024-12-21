package io.datadynamics.prometheus.metricfilter.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class QueryProfileDecoder {

    public static void main(String[] args) {
        String baseDir = null;
        if (args.length == 0) {
            baseDir = System.getProperty("user.dir");
        } else {
            baseDir = args[0];
        }

        System.out.println("Base Dir : " + baseDir);

        File[] files = new File(baseDir).listFiles();
        for (File f : files) {
            try {
                decode(f.getAbsolutePath());
            } catch (Exception e) {
                // Ignored
            }
        }
    }

    public static void decode(String filename) throws IOException {
        FileReader fr = new FileReader(filename);
        BufferedReader reader = new BufferedReader(fr);
        String row = reader.readLine();
        while (row != null) {
            Map map = ImpalaUtils.decodeQueryProfile(row);
            System.out.println(String.format(">> Query ID : %s\n%s\n", map.get("queryId"), map.get("queryProfile")));
            row = reader.readLine();
        }
    }
}
