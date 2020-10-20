package me.lolieg.utility116.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import org.bson.conversions.Bson;

import java.util.ArrayList;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class HomeCommand {
    //TODO fix order bug with home cmd
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated){
        dispatcher.register(literal("home")
                .executes(HomeCommand::home)
                .then(argument("number", IntegerArgumentType.integer(1, Utility116.config.maxHomes))
                        .executes(HomeCommand::home)));
        dispatcher.register(literal("sethome")
                .executes(HomeCommand::sethome)
                .then(argument("number", IntegerArgumentType.integer(1, Utility116.config.maxHomes))
                        .executes(HomeCommand::sethome)));
        dispatcher.register(literal("delhome")
                .executes(HomeCommand::delhome)
                .then(argument("number", IntegerArgumentType.integer(1, Utility116.config.maxHomes))
                        .executes(HomeCommand::delhome)));
        dispatcher.register(literal("listhomes")
                .executes(HomeCommand::listhomes));

    }

    private static int getNumber(CommandContext<ServerCommandSource> ctx){
        try{
            return IntegerArgumentType.getInteger(ctx, "number");
        }catch (IllegalArgumentException argumentException){
            return 1;
        }
    }


    public static int sethome(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        BsonArray cords = new BsonArray();
        cords.add(new BsonDouble(source.getPlayer().getX()));
        cords.add(new BsonDouble(source.getPlayer().getY()));
        cords.add(new BsonDouble(source.getPlayer().getZ()));
        cords.add(new BsonString(source.getWorld().getRegistryKey().getValue().toString()));

        int number = getNumber(ctx);

        Document player = new Document("uuid", new BsonString(source.getPlayer().getUuidAsString()));
        Document result = Utility116.collection.find(player).first();
        if(result != null){
            Document homes = (Document) result.get("homes");
            int number2 = number-1;
            if(number > 1 && !homes.containsKey(String.valueOf(number2))){
                source.sendError(new LiteralText("Set your home points in order please, the next number you should set is " + homes.size()+1 ));
                return Command.SINGLE_SUCCESS;
            }
            homes.append(String.valueOf(number), cords);
            result.append("homes", homes);
            Utility116.collection.replaceOne(player, result);
        }else{
            player = new Document("uuid", new BsonString(source.getPlayer().getUuidAsString())).append("homes", new Document(String.valueOf(number), cords));
            Utility116.collection.insertOne(player);
        }

        source.sendFeedback(new LiteralText("Successfully set home (" + number + ") here!"), false);
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

    public static int delhome(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {

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

    public static int listhomes(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Bson player = new Document("uuid", new BsonString(ctx.getSource().getPlayer().getUuidAsString()));
        Document result = Utility116.collection.find(player).first();
        if (result == null) {
            ctx.getSource().sendError(new LiteralText("No Home point set yet."));
            return Command.SINGLE_SUCCESS;
        }
        Document homes = (Document) result.get("homes");
        ctx.getSource().getPlayer().sendMessage(new LiteralText("§6<-- You have §c" + homes.size() + "§6 set! -->"), false);
        for(int i = 1; i < homes.size()+1; i++){
            ArrayList<?> home = (ArrayList<?>) homes.get(String.valueOf(i));

            ctx.getSource().getPlayer().sendMessage(new LiteralText("§6<-Home " + i + "->§8" + " X:" + Math.round((Double) home.get(0)) + " Y:" + Math.round((Double) home.get(1)) + " Z:" + Math.round((Double) home.get(2)) + " " + home.get(3)), false);

        }
        return Command.SINGLE_SUCCESS;
    }

}
