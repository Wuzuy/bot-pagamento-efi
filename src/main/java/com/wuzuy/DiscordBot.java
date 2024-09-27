package com.wuzuy;

import com.wuzuy.commands.PayCommands;
import com.wuzuy.database.DatabaseManager;
import com.wuzuy.transactions.TransactionChecker;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class DiscordBot {

    public static JDA jda;

    public static void main(String[] args) throws InterruptedException {


        jda = JDABuilder.createDefault("DISCORD_TOKEN")
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                .build();

        jda.addEventListener(
                new PayCommands()
        );

        jda.awaitReady();

        DatabaseManager.initializeDatabase();

        TransactionChecker checker = new TransactionChecker(jda);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(checker, 0, 10, TimeUnit.SECONDS);
    }
}
