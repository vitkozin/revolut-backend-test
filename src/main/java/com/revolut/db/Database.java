package com.revolut.db;

import java.io.*;
import java.sql.*;

public class Database {
    private static final String INIT_SQL = "/init.sql";

    public Database() {
        loadDriver();
        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            initDefaultValues(statement);
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error during initializing DB");
            System.exit(1);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:hsqldb:mem:db", "SA", "");
    }

    private void loadDriver() {
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBCDriver not found");
            System.exit(1);
        }
    }

    private void initDefaultValues(Statement statement) {
        try (InputStream inputStream = getClass().getResourceAsStream(INIT_SQL);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = bufferedReader.readLine();
            while (line != null) {
                statement.executeQuery(line);
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            System.err.println("Error during reading init.sql");
        } catch (SQLException e) {
            System.err.println("Error during SQL query executing");
        }
    }
}
