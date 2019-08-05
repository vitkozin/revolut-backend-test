package com.revolut.services;

import com.revolut.db.DAO;
import com.revolut.model.Account;
import com.revolut.exceptions.AccountNotExistException;

import java.sql.Connection;
import java.sql.SQLException;

public class AccountService extends Service {
    public static Account getAccount(long id, boolean forUpdate) throws SQLException, AccountNotExistException {
        try (Connection connection = database.getConnection()) {
            Account account = DAO.getAccount(connection, id, forUpdate);
            if (account == null) {
                throw new AccountNotExistException(String.format("Account %d not exist", id));
            } else {
                return account;
            }
        }
    }
}
