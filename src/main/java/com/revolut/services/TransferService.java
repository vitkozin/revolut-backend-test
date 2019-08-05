package com.revolut.services;

import com.revolut.db.DAO;
import com.revolut.exceptions.*;
import com.revolut.model.Account;
import com.revolut.model.Transfer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class TransferService extends Service {
    public static void processTransfer(Transfer transfer) throws TransferException, SQLException {
        long fromId = transfer.from;
        long toId = transfer.to;

        if (fromId == toId) {
            throw new SameAccountsException("Transfer should be between different accounts");
        }

        if (transfer.sum.compareTo(new BigDecimal(0)) < 1) {
            throw new ZeroTransferException("Transfer should be more than zero");
        }

        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            Account fromAccount;
            Account toAccount;
            if (fromId < toId) {
                fromAccount = DAO.getAccount(connection, fromId, true);
                toAccount = DAO.getAccount(connection, toId, true);
            } else {
                toAccount = DAO.getAccount(connection, toId, true);
                fromAccount = DAO.getAccount(connection, fromId, true);
            }
            if (fromAccount == null) {
                throw new AccountNotExistException(String.format("Account %d not exist", fromId));
            }
            if (toAccount == null) {
                throw new AccountNotExistException(String.format("Account %d not exist", toId));
            }

            BigDecimal sum = transfer.sum;
            boolean notEnoughMoney = fromAccount.balance.compareTo(sum) < 0;
            if (notEnoughMoney) {
                throw new NotEnoughMoneyException("Not enough money");
            }

            executeTransfer(transfer, connection, fromId, fromAccount, toAccount);
        }
    }

    private static void executeTransfer(Transfer transfer, Connection connection, long fromId,
                                        Account fromAccount, Account toAccount) throws SQLException {
        BigDecimal newValueFromAccount = fromAccount.balance.subtract(transfer.sum);
        DAO.updateAccountBalance(connection, fromId, newValueFromAccount);
        BigDecimal newValueToAccount = toAccount.balance.add(transfer.sum);
        DAO.updateAccountBalance(connection, transfer.to, newValueToAccount);
        connection.commit();
    }
}
