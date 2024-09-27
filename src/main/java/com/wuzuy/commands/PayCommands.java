package com.wuzuy.commands;

import com.google.gson.JsonObject;
import com.wuzuy.database.DatabaseManager;
import com.wuzuy.integrations.QRCodeGenerator;
import com.wuzuy.pix.PixApiClient;
import com.wuzuy.pix.TokenGenerator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.Color;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PayCommands extends ListenerAdapter {

    private static final ConcurrentHashMap<String, String> qrCodeMap = new ConcurrentHashMap<>();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().equalsIgnoreCase("!pay")) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Pagamento")
                    .setDescription("Clique no botão abaixo para pagar via Pix!")
                    .setColor(Color.GREEN);

            event.getChannel().sendMessageEmbeds(embed.build())
                    .addActionRow(Button.of(ButtonStyle.PRIMARY, "pagar_button", "Me pague!"))
                    .queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String customId = event.getComponentId();

        if (customId.equals("pagar_button")) {
            event.deferReply(true).queue();

            CompletableFuture.runAsync(() -> {
                try {
                    // Parâmetros para a criação da cobrança Pix
                    int expiracao = 3600; // 1 hora
                    String token = TokenGenerator.getAccessToken();
                    String valor = "9.00"; // Valor da cobrança
                    String infoPagador = "Pagamento referente ao serviço X";
                    String chave = "61b555ee-2975-433a-af8c-785f7608f2a2"; // Sua chave Pix

                    JsonObject response = PixApiClient.criarCobrancaPix(token, expiracao, valor, infoPagador, chave);

                    String servidorId = Objects.requireNonNull(event.getGuild()).getId();
                    String idCompra = response.get("txid").getAsString();
                    String idUsuario = event.getUser().getId();
                    String status = "Pendente";

                    DatabaseManager.insertTransaction(servidorId, idCompra, idUsuario, valor, status);

                    if (response.has("pixCopiaECola")) {
                        String pixCopia = response.get("pixCopiaECola").getAsString();

                        byte[] qrCodeImage = QRCodeGenerator.generateQRCodeImage(pixCopia, 300, 300);

                        FileUpload qrCodeFile = FileUpload.fromData(qrCodeImage, "qrcode.png");

                        String uniqueId = UUID.randomUUID().toString();

                        qrCodeMap.put(uniqueId, pixCopia);

                        Button showQrCodeButton = Button.primary("show_qrcode_" + uniqueId, "Mostrar QR Code");

                        event.getHook().sendMessage("Cobrança Pix criada com sucesso. Pix: \n\n")
                                .addFiles(qrCodeFile)
                                .addActionRow(showQrCodeButton)
                                .queue();
                    } else {
                        event.getHook().sendMessage("Campo 'pixCopiaECola' não encontrado na resposta.")
                                .setEphemeral(true)
                                .queue();
                    }
                } catch (Exception e) {
                    event.getHook().sendMessage("Erro ao criar a cobrança Pix: " + e.getMessage()).queue();
                    e.printStackTrace();
                }
            });

        } else if (customId.startsWith("show_qrcode_")) {
            String uniqueId = customId.substring("show_qrcode_".length());

            String pixCopiaECola = qrCodeMap.get(uniqueId);

            if (pixCopiaECola != null) {
                event.reply("Aqui está o QR Code em texto:\n```\n" + pixCopiaECola + "\n```")
                        .setEphemeral(true)
                        .queue();

                qrCodeMap.remove(uniqueId);
            } else {
                event.reply("QR Code não encontrado ou já foi exibido.")
                        .setEphemeral(true)
                        .queue();
            }
        }
    }
}
