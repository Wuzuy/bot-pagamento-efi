package com.wuzuy.database;

import com.wuzuy.models.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:status.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("Driver SQLite JDBC carregado com sucesso.");
        } catch (ClassNotFoundException e) {
            System.err.println("Erro ao carregar o driver SQLite JDBC: " + e.getMessage());
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                String sql = "CREATE TABLE IF NOT EXISTS transacoes (\n"
                        + "    servidor_id TEXT NOT NULL,\n"
                        + "    id_compra TEXT NOT NULL,\n"
                        + "    id_usuario TEXT NOT NULL,\n"
                        + "    valor_cobranca TEXT NOT NULL,\n"
                        + "    status TEXT NOT NULL\n"
                        + ");";
                Statement stmt = conn.createStatement();
                stmt.execute(sql);
                System.out.println("Banco de dados inicializado com sucesso.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void insertTransaction(String servidorId, String idCompra, String idUsuario, String valorCobranca, String status) {
        String sql = "INSERT INTO transacoes(servidor_id, id_compra, id_usuario, valor_cobranca, status) VALUES(?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, servidorId);
            pstmt.setString(2, idCompra);
            pstmt.setString(3, idUsuario);
            pstmt.setString(4, valorCobranca);
            pstmt.setString(5, status);
            pstmt.executeUpdate();
            System.out.println("Transação inserida com sucesso.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Novo método para obter transações pendentes
    public static List<Transaction> getPendingTransactions() {
        String sql = "SELECT * FROM transacoes WHERE status = 'Pendente'";
        List<Transaction> pendingTransactions = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Transaction transaction = new Transaction(
                        rs.getString("servidor_id"),
                        rs.getString("id_compra"),
                        rs.getString("id_usuario"),
                        rs.getString("valor_cobranca"),
                        rs.getString("status")
                );
                pendingTransactions.add(transaction);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return pendingTransactions;
    }

    public static void updateTransactionStatus(String idCompra, String newStatus) {
        String sql = "UPDATE transacoes SET status = ? WHERE id_compra = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, idCompra);
            pstmt.executeUpdate();
            System.out.println("Transação atualizada com sucesso.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
