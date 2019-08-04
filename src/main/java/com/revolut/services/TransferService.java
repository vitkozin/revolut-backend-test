package com.revolut.services;

import com.revolut.db.DAO;
import com.revolut.model.Account;
import com.revolut.model.Transfer;
import com.revolut.exceptions.AccountNotExistException;
import com.revolut.exceptions.NotEnoughMoneyException;
import com.revolut.exceptions.TransferException;
import com.revolut.exceptions.ZeroTransferException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class TransferService extends Service {
    public static synchronized void processTransfer(Transfer transfer) throws TransferException, SQLException {
        long fromId = transfer.from;
        long toId = transfer.to;

        if (transfer.sum.compareTo(new BigDecimal(0)) < 1) {
            throw new ZeroTransferException("Transfer should be more than zero");
        }

        try (Connection connection = database.getConnection()) {
            Account fromAccount = DAO.getAccount(connection, fromId);
            if (fromAccount == null) {
                throw new AccountNotExistException(String.format("Account %d not exist", fromId));
            }
            Account toAccount = DAO.getAccount(connection, toId);
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
        connection.setAutoCommit(false);
        BigDecimal newValueFromAccount = fromAccount.balance.subtract(transfer.sum);
        DAO.updateAccountBalance(connection, fromId, newValueFromAccount);
        BigDecimal newValueToAccount = toAccount.balance.add(transfer.sum);
        DAO.updateAccountBalance(connection, transfer.to, newValueToAccount);
        connection.commit();
    }
}
