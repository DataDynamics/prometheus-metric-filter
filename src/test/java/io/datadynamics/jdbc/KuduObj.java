package io.datadynamics.jdbc;

import lombok.Data;

@Data
public class KuduObj {

    private String index;

    @Override
    public String toString() {
        return "(" +
                "'" + "company" + index + "'," +
                "'" + "gbm" + index + "'," +
                "'2024-11-0" + (index.hashCode() % 4 + 1) + "'," +
                "'" + "datatype" + index + "'," +
                "'" + "isvalid" + index + "'," +
                "'" + "sono" + index + "'," +
                "'" + "soitem" + index + "'," +
                "'" + "sosche" + index + "'," +
                "'" + "documentno" + index + "'," +
                "'" + "documentitem" + index + "'," +
                "'" + "aptdoc" + index + "'," +
                "'" + "dmdship" + index + "'," +
                "'" + "dmdlnitem" + index + "'" +
                ")";
    }
}
