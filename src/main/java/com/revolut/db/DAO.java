package com.revolut.db;

import com.revolut.model.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DAO {
    public static Account getAccount(Connection connection, long id) throws SQLException {
        Account account = null;
        Statement statement = connection.createStatement();
        String sql = String.format("SELECT balance FROM account WHERE id = %d;", id);
        ResultSet resultSet = statement.executeQuery(sql);
        statement.close();
        if (resultSet.next()) {
            account = new Account(id, resultSet.getBigDecimal("balance"));
        }
        return account;
    }

    public static void updateAccountBalance(Connection connection, long id, BigDecimal sum)
            throws SQLException {
        Statement statement = connection.createStatement();
        String sql = String.format("UPDATE account SET balance = %s WHERE id = %d;", sum, id);
        statement.executeUpdate(sql);
        statement.close();
    }
}
