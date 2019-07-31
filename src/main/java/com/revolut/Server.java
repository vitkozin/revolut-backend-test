package com.revolut;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.revolut.db.DAO;
import com.revolut.db.Database;
import com.revolut.model.Account;
import com.revolut.model.Transfer;
import com.revolut.response.Response;
import com.revolut.response.Status;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import static spark.Spark.get;
import static spark.Spark.post;

public class Server {
    private static Database database = new Database();

    public static void main(String[] args) {
        Logger.getRootLogger().setLevel(Level.INFO);
        BasicConfigurator.configure();
        Server.start();
    }

    static void reloadDatabase() {
        database = new Database();
    }

    static void start() {
        accountEndPoint();
        transferEndPoint();
    }

    private static void transferEndPoint() {
        post("/transfer", (request, response) -> {
            response.type("application/json");

            String responseBody;
            Gson gson = new Gson();
            try (Connection connection = database.getConnection()) {
                Transfer transfer = gson.fromJson(request.body(), Transfer.class);

                if (transfer.sum.compareTo(new BigDecimal(0)) < 1) {
                    response.status(400);
                    return gson.toJson(new Response(Status.ERROR, "Transfer should be more than zero"));
                }

                responseBody = processTransfer(response, connection, transfer);
            } catch (JsonParseException e) {
                response.status(400);
                responseBody = gson.toJson(new Response(Status.ERROR, "Incorrect transfer"));
            } catch (SQLException e) {
                response.status(500);
                return gson.toJson(new Response(Status.ERROR, "Internal Error"));
            }
            return responseBody;
        });
    }

    private static synchronized String processTransfer(spark.Response response, Connection connection, Transfer transfer)
            throws SQLException {
        Gson gson = new Gson();
        long fromId = transfer.from;
        long toId = transfer.to;

        Account fromAccount = DAO.getAccount(connection, fromId);
        if (fromAccount == null) {
            response.status(400);
            return gson.toJson(new Response(Status.ERROR, "Account " + fromId + " not exist"));
        }
        Account toAccount = DAO.getAccount(connection, toId);
        if (toAccount == null) {
            response.status(400);
            return gson.toJson(new Response(Status.ERROR, "Account " + toId + " not exist"));
        }

        BigDecimal sum = transfer.sum;
        boolean notEnoughMoney = fromAccount.balance.compareTo(sum) < 0;
        if (notEnoughMoney) {
            response.status(400);
            return gson.toJson(new Response(Status.ERROR, "Not enough money"));
        }

        executeTransfer(transfer, connection, fromId, fromAccount, toAccount);
        return gson.toJson(new Response(Status.SUCCESS));
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

    private static void accountEndPoint() {
        get("/account/:id", (request, response) -> {
            response.type("application/json");

            Account account;
            Gson gson = new Gson();
            String idParam = request.params(":id");
            try (Connection connection = database.getConnection()) {
                long id = Long.parseLong(idParam);
                account = DAO.getAccount(connection, id);
                if (account == null) {
                    response.status(404);
                    return gson.toJson(new Response(Status.ERROR, "Account " + idParam + " not exist"));
                } else {
                    return gson.toJson(account);
                }
            } catch (NumberFormatException e) {
                response.status(400);
                return gson.toJson(new Response(Status.ERROR, "Incorrect id parameter"));
            } catch (SQLException e) {
                response.status(500);
                return gson.toJson(new Response(Status.ERROR, "Internal Error"));
            }
        });
    }
}
