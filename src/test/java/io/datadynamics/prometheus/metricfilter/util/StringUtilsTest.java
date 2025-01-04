package io.datadynamics.prometheus.metricfilter.util;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

public class StringUtilsTest {

    @Test
    public void hostname() {
        Assert.isTrue(StringUtils.getHostname("http://hdw3.datalake.net:25000/sessions").equalsIgnoreCase("hdw3.datalake.net:25000"));
    }

}