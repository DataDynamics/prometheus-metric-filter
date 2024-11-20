package io.datadynamics.jdbc;

import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Haneul, Kim
 * @version 1.0.0
 * @since 2024-11-20
 */
public class KuduInsertCallable implements Callable<Integer> {
  public static final String CONNECTION_URL = "jdbc:impala://hdw1.datalake.net:21050/default";
  public static final String JDBC_DRIVER = "com.cloudera.impala.jdbc.Driver";

  private final int id;
  private final int dataCount;
  private final int chunkSize;

  public KuduInsertCallable(int id, int dataCount, int chunkSize) {
    this.id = id;
    this.dataCount = dataCount;
    this.chunkSize = chunkSize;
  }

  @Override
  public Integer call() throws Exception {
    try {
      Class.forName(JDBC_DRIVER);
      Connection conn = DriverManager.getConnection(CONNECTION_URL, "impala", "impala");
      Statement stmt = conn.createStatement();

      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < dataCount; i += chunkSize) {
        sb.setLength(0);
        sb.append("insert into kududb.dyn_salesorder_dp values\n");
        KuduObj kuduObj;
        List<String> objs = new ArrayList<>(chunkSize);
        for (int j = 0; j < chunkSize; j++) {
          kuduObj = new KuduObj();
          kuduObj.setIndex(String.valueOf(id + i + j));
          String string = kuduObj.toString();
          objs.add(string);
        }
        sb.append(StringUtils.join(objs, ", "));

        sb.append(";");
        String sql = sb.toString();
        int result = stmt.executeUpdate(sql);
        // if (result < 1 || i % 10000 == 0) {
        System.out.println("id = " + id + ", insert result = " + result + ", inserted = " + ((i + 1) * chunkSize));
      }
      conn.close();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return 0;
  }
}
