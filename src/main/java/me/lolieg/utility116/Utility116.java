package me.lolieg.utility116;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.lolieg.utility116.commands.HomeCommand;
import me.lolieg.utility116.utils.Config;
import me.lolieg.utility116.utils.DatabaseUtil;
import me.lolieg.utility116.utils.ScoreboardUtil;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.bson.Document;

public class Utility116 implements ModInitializer {

    public static MongoCollection<Document> collection;
    public static Config config;

    @Override
    public void onInitialize() {
        System.out.println("Starting Utility1.16");

        AutoConfig.register(Config.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(Config.class).getConfig();

        MongoDatabase db = DatabaseUtil.connect(config.databaseUrl);

        collection = db.getCollection("homes");
            register();
    }

    public static void register(){
        CommandRegistrationCallback.EVENT.register(HomeCommand::register);

        ServerTickEvents.END_SERVER_TICK.register((t) -> {
            if (System.currentTimeMillis() % 8 == 0) {
                ScoreboardUtil.tick(t);
            }});

        ServerLifecycleEvents.SERVER_STARTED.register(ScoreboardUtil::start);
    }
}
