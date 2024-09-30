package com.wuzuy;

import com.wuzuy.commands.PayCommands;
import com.wuzuy.database.DatabaseManager;
import com.wuzuy.tasks.PaymentChecker;
import com.wuzuy.telegram.TelegramBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class DiscordBot {

    public static JDA jda;
    public static TelegramBot telegramBot;


    public static void main(String[] args) throws InterruptedException {

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        jda = JDABuilder.createDefault("MTI5MDI4NDAzNzU0OTcxOTY2NA.G_xbc7.gGosDrIQ7k5HDvX4nHVO-11c1sB994Vlrt79o0")
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                .build();

        jda.addEventListener(
                new PayCommands()
        );

        jda.awaitReady();

        try {
            String botToken = "8153189510:AAFLaMaKbylo6Ab0Gq6lYd0MAMhdnWkc4hs";
            String botUsername = "wuzuy_bot";
            String groupChatId = "2380871902";

            telegramBot = new TelegramBot(botToken, botUsername, groupChatId);

            // Registrar o bot (faça isso apenas uma vez na inicialização do seu aplicativo)
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBot);

        } catch (Exception e) {
            e.printStackTrace();
        }

        DatabaseManager.initializeDatabase();
        System.out.println("Bot está pronto e conectado!");

        PaymentChecker paymentChecker = new PaymentChecker(scheduler, jda, telegramBot);
        paymentChecker.start();
    }
}