package com.revolut;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import spark.Spark;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class ServerTest {

    @BeforeClass
    public static void init() {
        Logger.getRootLogger().setLevel(Level.INFO);
        BasicConfigurator.configure();
        Server.start();
        Spark.awaitInitialization();
    }

    @Before
    public void reloadDatabase() {
        Server.reloadDatabase();
    }

    @Test
    public void testGetAccount() {
        Response response = getAccount("1");
        assertEquals(200, response.code);
        assertEquals("{\"id\":1,\"balance\":0.00}", response.body);
    }

    @Test
    public void testNotExistAccount() {
        String id = "2";
        Response response = getAccount(id);
        assertEquals(200, response.code);
        assertEquals("{\"status\":\"ERROR\",\"message\":\"Account " + 2 + " not exist\"}", response.body);
    }

    @Test
    public void testIncorrectId() {
        Response response = getAccount("O");
        assertEquals(400, response.code);
        assertEquals("{\"status\":\"ERROR\",\"message\":\"Incorrect id parameter\"}", response.body);
    }

    @Test
    public void testIncorrectTransfer() {
        Response response = transfer("0", "1", "0.00e");
        assertEquals(400, response.code);
        assertEquals("{\"status\":\"ERROR\",\"message\":\"Incorrect transfer\"}", response.body);
    }

    @Test
    public void testZeroTransfer() {
        Response response = transfer("0", "1", "0");
        assertEquals(400, response.code);
        assertEquals("{\"status\":\"ERROR\",\"message\":\"Transfer should be more than zero\"}", response.body);
    }

    @Test
    public void testNotEnoughMoneyTransfer() {
        Response response = transfer("1", "0", "0.01");
        assertEquals(200, response.code);
        assertEquals("{\"status\":\"ERROR\",\"message\":\"Not enough money\"}", response.body);
    }

    @Test
    public void testTransferFromNotExistAccount() {
        Response response = transfer("2", "0", "0.01");
        assertEquals(200, response.code);
        assertEquals("{\"status\":\"ERROR\",\"message\":\"Account 2 not exist\"}", response.body);
    }

    @Test
    public void testTransferToNotExistAccount() {
        Response response = transfer("1", "2", "0.01");
        assertEquals(200, response.code);
        assertEquals("{\"status\":\"ERROR\",\"message\":\"Account 2 not exist\"}", response.body);
    }

    @Test
    public void testTransfer() {
        Response response = transfer("0", "1", "0.01");
        assertEquals(200, response.code);
        assertEquals("{\"status\":\"SUCCESS\"}", response.body);

        response = getAccount("1");
        assertEquals(200, response.code);
        assertEquals("{\"id\":1,\"balance\":0.01}", response.body);

        response = getAccount("0");
        assertEquals(200, response.code);
        assertEquals("{\"id\":0,\"balance\":999.98}", response.body);
    }

    private Response transfer(String from, String to, String sum) {
        Response result = null;
        HttpURLConnection connection;
        try {
            URL url = new URL("http://localhost:4567/transfer");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);
            String jsonInputString = String.format("{\"from\": %s, \"to\": %s, \"sum\":%s}", from, to, sum);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            InputStream inputStream = getInputStream(connection);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                result = new Response(connection.getResponseCode(), response.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private InputStream getInputStream(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        InputStream inputStream;
        if (200 <= responseCode && responseCode <= 299) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }
        return inputStream;
    }

    private Response getAccount(String id) {
        Response result = null;
        HttpURLConnection connection;
        try {
            URL url = new URL(String.format("http://localhost:4567/account/%s", id));
            connection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = getInputStream(connection);

            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder response = new StringBuilder();
            String responseLine;

            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine);
            }

            br.close();

            result = new Response(connection.getResponseCode(), response.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static class Response {
        int code;
        String body;

        Response(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }
}
