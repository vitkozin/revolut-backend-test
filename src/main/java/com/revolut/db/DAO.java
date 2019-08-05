package com.revolut.db;

import com.revolut.model.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DAO {
    public static Account getAccount(Connection connection, long id, boolean forUpdate) throws SQLException {
        Account account = null;
        try (Statement statement = connection.createStatement()) {
            String sql =
                    String.format("SELECT balance FROM account WHERE id = %d %s;", id, forUpdate ? "FOR UPDATE" : "");
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                account = new Account(id, resultSet.getBigDecimal("balance"));
            }
        }
        return account;
    }

    public static void updateAccountBalance(Connection connection, long id, BigDecimal sum)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            String sql = String.format("UPDATE account SET balance = %s WHERE id = %d;", sum, id);
            statement.executeUpdate(sql);
        }
    }
}
