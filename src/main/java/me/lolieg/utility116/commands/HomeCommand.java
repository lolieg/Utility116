package me.lolieg.utility116.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mongodb.lang.Nullable;
import me.lolieg.utility116.Utility116;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.bson.BsonArray;
import org.bson.BsonDouble;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.function.BiConsumer;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class HomeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated){
        dispatcher.register(literal("home")
                .executes(HomeCommand::home)
                .then(argument("number", IntegerArgumentType.integer(1, Utility116.config.maxHomes))
                        .executes(HomeCommand::home)));
        dispatcher.register(literal("sethome")
                .executes(HomeCommand::setHome)
                .then(argument("number", IntegerArgumentType.integer(1, Utility116.config.maxHomes))
                        .executes(HomeCommand::setHome)
                            .then(argument("name", StringArgumentType.greedyString())
                                .executes(HomeCommand::setHome))));
        dispatcher.register(literal("sethomename")
                .then(argument("number", IntegerArgumentType.integer(1, Utility116.config.maxHomes))
                        .then(argument("name", StringArgumentType.greedyString())
                                .executes(HomeCommand::setName))));
        dispatcher.register(literal("delhome")
                .executes(HomeCommand::delHome)
                .then(argument("number", IntegerArgumentType.integer(1, Utility116.config.maxHomes))
                        .executes(HomeCommand::delHome)));
        dispatcher.register(literal("listhomes")
                .executes(HomeCommand::listHomes));

    }

    private static int getNumber(CommandContext<ServerCommandSource> ctx){
        try{
            return IntegerArgumentType.getInteger(ctx, "number");
        }catch (IllegalArgumentException argumentException){
            return 1;
        }
    }

    private static String getName(CommandContext<ServerCommandSource> ctx){
        try{
            return StringArgumentType.getString(ctx, "name");
        }catch (IllegalArgumentException argumentException){
            return "";
        }
    }


    public static int setHome(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        int number = getNumber(ctx);
        String name = getName(ctx);

        BsonArray cords = new BsonArray();
        cords.add(new BsonDouble(source.getPlayer().getX()));
        cords.add(new BsonDouble(source.getPlayer().getY()));
        cords.add(new BsonDouble(source.getPlayer().getZ()));
        cords.add(new BsonString(source.getWorld().getRegistryKey().getValue().toString()));
        if(name != null && !name.isEmpty()){
            cords.add(new BsonString(name));
        }

        Document player = new Document("uuid", new BsonString(source.getPlayer().getUuidAsString()));
        Document result = Utility116.collection.find(player).first();
        if(result != null){
            Document homes = (Document) result.get("homes");
            homes.append(String.valueOf(number), cords);
            result.append("homes", homes);
            Utility116.collection.replaceOne(player, result);
        }else{
            player = new Document("uuid", new BsonString(source.getPlayer().getUuidAsString())).append("homes", new Document(String.valueOf(number), cords));
            Utility116.collection.insertOne(player);
        }

        source.sendFeedback(new LiteralText("Successfully set home (" + number + ") here!" + ((name != null && !name.isEmpty()) ? " name: " + name : "")), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int setName(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        int number = getNumber(ctx);
        String name = getName(ctx);

        Document player = new Document("uuid", new BsonString(ctx.getSource().getPlayer().getUuidAsString()));
        Document result = Utility116.collection.find(player).first();

        if(result == null){
            ctx.getSource().sendError(new LiteralText("You have no home points set!"));
            return Command.SINGLE_SUCCESS;
        }
        Document homes = (Document) result.get("homes");
        if(!homes.containsKey(String.valueOf(number))){
            ctx.getSource().sendError(new LiteralText("This home (" + number + ") has not been set yet!"));
            return Command.SINGLE_SUCCESS;
        }
        ArrayList home = (ArrayList) homes.get(String.valueOf(number));
        if(home.size() == 4){
            home.add(new BsonString(name));
        }else{
            home.add(4, new BsonString(name));
        }
        result.append("homes", homes);
        Utility116.collection.replaceOne(player, result);

        ctx.getSource().sendFeedback(new LiteralText("Successfully set name " + name + " for home (" + number + ")!"), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int home(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();

        int number = getNumber(ctx);

        Bson player = new Document("uuid", new BsonString(source.getPlayer().getUuidAsString()));
        Document result = Utility116.collection.find(player).first();
        if (result == null) {
            source.sendError(new LiteralText("No Home point set yet."));
            return Command.SINGLE_SUCCESS;
        }
        ArrayList<?> cords;
        if (((Document)result.get("homes")).containsKey(String.valueOf(number))){
            cords = (ArrayList<?>) ((Document) result.get("homes")).get(String.valueOf(number));

            ServerWorld serverWorld = source.getMinecraftServer().getWorld(RegistryKey.of(Registry.DIMENSION, Identifier.tryParse((String) cords.get(3))));

            source.getPlayer().teleport(serverWorld, (Double) cords.get(0), (Double) cords.get(1), (Double) cords.get(2), source.getPlayer().yaw, source.getPlayer().pitch);

            for(int i = 0; i <360; i+=2){
                Vec3d flameloc = source.getPlayer().getPos();
                flameloc.add(0,0,Math.cos(i)*2);
                flameloc.add(Math.sin(i)*2, 0, 0);
                source.getWorld().spawnParticles(ParticleTypes.REVERSE_PORTAL, flameloc.x, flameloc.y, flameloc.z, 4,0,0,0,0.5F);
            }
            source.getWorld().playSound(null, source.getPlayer().getX(), source.getPlayer().getY(), source.getPlayer().getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.AMBIENT, 1.0F, 1.0F);


        }else{
            source.sendError(new LiteralText("This home (" + number + ") has not been set yet!"));
        }

        return Command.SINGLE_SUCCESS;
    }

    public static int delHome(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {

        int number = getNumber(ctx);
        Bson player = new Document("uuid", new BsonString(ctx.getSource().getPlayer().getUuidAsString()));
        Document result = Utility116.collection.find(player).first();
        if (result == null) {
            ctx.getSource().sendError(new LiteralText("No Home point set yet."));
            return Command.SINGLE_SUCCESS;
        }

        Document homes = (Document) result.get("homes");

        if(homes.containsKey(String.valueOf(number))){
            homes.remove(String.valueOf(number));
            Utility116.collection.replaceOne(player, result);
            ctx.getSource().sendFeedback(new LiteralText("Successfully deleted home (" + number + ")!"), false);
        }else{
            ctx.getSource().sendError(new LiteralText("home (" + number + ") has not been set!"));
        }
        return Command.SINGLE_SUCCESS;
    }


    private static int keyByValue(Document doc, Object value){
        for(String object : doc.keySet()){
            if(doc.get(object).equals(value)){
                return Integer.valueOf(object);
            }
        }
        return 0;
    }

    public static int listHomes(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Bson player = new Document("uuid", new BsonString(ctx.getSource().getPlayer().getUuidAsString()));
        Document result = Utility116.collection.find(player).first();
        if (result == null) {
            ctx.getSource().sendError(new LiteralText("No Home point set yet."));
            return Command.SINGLE_SUCCESS;
        }
        Document homes = (Document) result.get("homes");
        ctx.getSource().getPlayer().sendMessage(new LiteralText("§6<-- You have §c" + homes.size() + "§6 set! -->"), false);
        for(Object home:homes.values()){

            int i = keyByValue(homes, home);
            ctx.getSource().getPlayer().sendMessage(new LiteralText("§6<-Home " + i + "->§8" + " X:" + Math.round((Double) ((ArrayList) home).get(0)) + " Y:" + Math.round((Double) ((ArrayList) home).get(1)) + " Z:" + Math.round((Double) ((ArrayList) home).get(2)) + " " + ((ArrayList) home).get(3) + ((((ArrayList) home).size() > 4) ? " name: " + ((ArrayList) home).get(4) : "")), false);
        }
        return Command.SINGLE_SUCCESS;
    }

}
