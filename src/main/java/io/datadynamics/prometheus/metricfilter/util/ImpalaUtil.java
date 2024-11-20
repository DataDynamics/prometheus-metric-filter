package io.datadynamics.prometheus.metricfilter.util;

import org.jsoup.Jsoup;

import javax.swing.text.Document;

public class ImpalaUtil {

    public void getSessions() {
        Document doc = Jsoup.connect("http://jsoup.org").get();
    }
}
