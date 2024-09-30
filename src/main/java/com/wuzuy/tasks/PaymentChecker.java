package com.wuzuy.tasks;

import com.google.gson.JsonObject;
import com.wuzuy.database.DatabaseManager;
import com.wuzuy.models.PaymentData;
import com.wuzuy.pix.PixApiClient;
import com.wuzuy.pix.TokenGenerator;
import com.wuzuy.telegram.TelegramBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PaymentChecker {

    private final ScheduledExecutorService scheduler;
    private final JDA jda;
    private final TelegramBot telegramBot;

    public PaymentChecker(ScheduledExecutorService scheduler, JDA jda, TelegramBot telegramBot) {
        this.scheduler = scheduler;
        this.jda = jda;
        this.telegramBot = telegramBot;
    }

    /**
     * Inicia a tarefa agendada para verificar pagamentos pendentes a cada 1 minuto.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkPayments, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Verifica o status dos pagamentos pendentes e envia mensagens aos usuários se necessário.
     */
    private void checkPayments() {
        try {
            // Obter pagamentos que estão com status 'Criado' e mensagem ainda não enviada
            List<PaymentData> pendingPayments = DatabaseManager.getPaymentsToCheck();

            if (pendingPayments.isEmpty()) {
                return;
            }

            String token = TokenGenerator.getAccessToken();

            for (PaymentData payment : pendingPayments) {
                // Verificar o status do pagamento via API Pix
                JsonObject response = PixApiClient.consultarCobrancaPix(token, payment.getIdCompra());

                if (response != null && response.has("status")) {
                    String status = response.get("status").getAsString();

                    if (status.equalsIgnoreCase("CONCLUIDA")) {
                        // Atualizar o status do pagamento no banco de dados
                        DatabaseManager.updatePagamentoStatus(payment.getUniqueId(), "Concluída");

                        // Enviar a mensagem com o produto se ainda não foi enviada
                        if (payment.getMessageSent() == 0) {
                            sendProductMessage(payment);
                            // Marcar que a mensagem foi enviada
                            DatabaseManager.updateMessageSentStatus(payment.getUniqueId(), 1);
                        }
                    } else if (status.equalsIgnoreCase("REMOVIDA_PELO_USUARIO_RECEBEDOR") ||
                            status.equalsIgnoreCase("EXPIRADA")) {
                        // Atualizar o status do pagamento no banco de dados
                        DatabaseManager.updatePagamentoStatus(payment.getUniqueId(), status);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Envia uma mensagem privada ao usuário com o produto após a confirmação do pagamento.
     *
     * @param payment Objeto PaymentData contendo os detalhes do pagamento.
     */
    private void sendProductMessage(PaymentData payment) {
        try {
            // Obter o usuário pelo ID
            User user = jda.getUserById(payment.getUserId());
            if (user != null) {
                // Gerar o link único do Telegram
                String linkProduto = telegramBot.generateUniqueInviteLink();

                if (linkProduto != null) {
                    // Enviar a mensagem privada com o produto
                    user.openPrivateChannel().queue(channel -> {
                        channel.sendMessage("Obrigado pelo seu pagamento! Aqui está o seu produto: " + linkProduto).queue();
                    });
                } else {
                    // Tratar erro ao gerar o link
                    user.openPrivateChannel().queue(channel -> {
                        channel.sendMessage("Obrigado pelo seu pagamento! Porém, ocorreu um erro ao gerar o link do produto. Por favor, entre em contato com o suporte.").queue();
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
