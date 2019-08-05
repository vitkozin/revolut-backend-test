package com.revolut;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.revolut.model.Account;
import com.revolut.model.Transfer;
import com.revolut.response.Response;
import com.revolut.response.Status;
import com.revolut.services.AccountService;
import com.revolut.services.TransferService;
import com.revolut.exceptions.AccountNotExistException;
import com.revolut.exceptions.TransferException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.sql.SQLException;

import static spark.Spark.get;
import static spark.Spark.post;

public class Server {
    public static void main(String[] args) {
        Logger.getRootLogger().setLevel(Level.INFO);
        BasicConfigurator.configure();
        Server.start();
    }

    static void start() {
        accountEndPoint();
        transferEndPoint();
    }

    private static void transferEndPoint() {
        post("/transfer", (request, response) -> {
            response.type("application/json");
            Gson gson = new Gson();
            try {
                Transfer transfer = gson.fromJson(request.body(), Transfer.class);
                TransferService.processTransfer(transfer);
                return gson.toJson(new Response(Status.SUCCESS));
            } catch (JsonParseException e) {
                response.status(400);
                return gson.toJson(new Response(Status.ERROR, "Incorrect transfer"));
            } catch (SQLException e) {
                response.status(500);
                return gson.toJson(new Response(Status.ERROR, "Internal Error"));
            } catch (TransferException e) {
                response.status(400);
                return gson.toJson(new Response(Status.ERROR, e.getMessage()));
            }
        });
    }

    private static void accountEndPoint() {
        get("/account/:id", (request, response) -> {
            response.type("application/json");

            Account account;
            Gson gson = new Gson();
            String idParam = request.params(":id");
            try {
                long id = Long.parseLong(idParam);
                account = AccountService.getAccount(id, false);
                return gson.toJson(account);
            } catch (NumberFormatException e) {
                response.status(400);
                return gson.toJson(new Response(Status.ERROR, "Incorrect id parameter"));
            } catch (SQLException e) {
                response.status(500);
                return gson.toJson(new Response(Status.ERROR, "Internal Error"));
            } catch (AccountNotExistException e) {
                response.status(404);
                return gson.toJson(new Response(Status.ERROR, e.getMessage()));
            }
        });
    }
}
