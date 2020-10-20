package me.lolieg.utility116.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class DatabaseUtil {

    public static MongoDatabase connect(String url) {
        if(url.length() < 1){
            System.out.println("PLEASE ENTER A DATABASE URL (MONGODB)!");
        }
        MongoClient mongoClient = MongoClients.create(url);
        return mongoClient.getDatabase("mc");
    }
}
