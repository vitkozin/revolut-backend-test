package com.revolut.services;

import com.revolut.db.Database;

public abstract class Service {
    static Database database = new Database();

    public static void reloadDatabase() {
        database = new Database();
    }
}
