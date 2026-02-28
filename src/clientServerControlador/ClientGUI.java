package clientServerControlador;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.*;
import org.json.*;

public class ClientGUI extends JFrame {
    private static final String SERVIDOR = "localhost";
    private static final int PORTA = 5000;
    private DefaultTableModel model;
    private JTable table;
    private JTextField campoIntervalo;
    private Timer timer;
    private JCheckBox boxAtualizacaoAutomatica;

    public ClientGUI() {
        setTitle("Monitoramento de Dispositivos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Define o tamanho preferido da janela (pode ajustar conforme desejar)
        setPreferredSize(new Dimension(800, 500));

        //Tabela para dispositivos e valores
        model = new DefaultTableModel(new Object[]{"Dispositivo", "Valor Atual"}, 0);
        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        //Campo e botões
        campoIntervalo = new JTextField("5", 5);
        JButton btnBuscar = new JButton("Buscar dispositivo");
        JButton btnAlterar = new JButton("Alterar atuador");
        JButton btnShutdown = new JButton("Desligar servidor");
        boxAtualizacaoAutomatica = new JCheckBox("Atualização automática", true);

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Intervalo:"));
        topPanel.add(campoIntervalo);
        topPanel.add(boxAtualizacaoAutomatica);
        topPanel.add(btnBuscar);
        topPanel.add(btnAlterar);
        topPanel.add(btnShutdown);
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        //Ativar/desativar atualização automática
        boxAtualizacaoAutomatica.addActionListener(e -> {
            if (boxAtualizacaoAutomatica.isSelected()) {
                definirTimer();
            } else if (timer != null) {
                timer.stop();
            }
        });

        //Atualizar intervalo da atualização automática
        campoIntervalo.addActionListener(e -> definirTimer());

        //Buscar valor de um dispositivo específico
        btnBuscar.addActionListener(e -> {
            String nome = JOptionPane.showInputDialog(this, "Nome do dispositivo:");
            if (nome != null && !nome.isEmpty()) {
                try {
                    JSONObject getReq = new JSONObject().put("cmd", "get_req").put("place", nome);
                    JSONObject getResp = enviar(getReq);
                    if (getResp.has("value")) {
                        JOptionPane.showMessageDialog(this, "Valor de " + nome + ": " + getResp.get("value"));
                    } else {
                        mostrarErro("Dispositivo não encontrado.");
                    }
                } catch (Exception ex) {
                    mostrarErro("Erro ao consultar dispositivo: " + ex.getMessage());
                }
            }
        });

        //Alterar valor de atuador pelo botão
        btnAlterar.addActionListener(e -> {
        	String atuador = null;
            int row = table.getSelectedRow();
            if (row >= 0) {
                atuador = model.getValueAt(row, 0).toString();
            } else {
                atuador = JOptionPane.showInputDialog(this, "Nome do atuador:");
            }
            if (atuador != null && !atuador.isEmpty()) {
                String valorStr = JOptionPane.showInputDialog(this, "Novo valor (ex: on, off, 22.5):");
                if (valorStr != null) {
                    alterarAtuador(atuador, valorStr);
                    atualizarTabela();
                }
            }
        });
        
        //Enviar shutdown para o servidor
        btnShutdown.addActionListener(e -> desligarServidor());



        definirTimer();
        atualizarTabela();

        //Ajusta a janela para caber os componentes e centraliza na tela
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    //Inicia/atualiza o Timer para atualização automática
    private void definirTimer() {
        try {
            int intervalo = Integer.parseInt(campoIntervalo.getText());
            if (timer != null) timer.stop();
            if (boxAtualizacaoAutomatica.isSelected()) {
                timer = new Timer(intervalo * 1000, e -> atualizarTabela());
                timer.start();
            }
        } catch (NumberFormatException ex) {
            mostrarErro("Intervalo inválido!");
        }
    }

    //Atualiza tabela com todos os dispositivos e valores
    private void atualizarTabela() {
        try {
            JSONObject getAllReq = new JSONObject().put("cmd", "get_req").put("place", "all");
            JSONObject resp = enviar(getAllReq);

            JSONArray places = resp.getJSONArray("place");
            JSONArray values = resp.getJSONArray("value");
            model.setRowCount(0);
            for (int i = 0; i < places.length(); i++) {
                model.addRow(new Object[]{places.getString(i), values.get(i)});
            }
        } catch (Exception e) {
            mostrarErro("Erro ao atualizar tabela: " + e.getMessage());
        }
    }

    //Altera valor de um atuador (usado apenas pelo botão)
    private void alterarAtuador(String dispositivo, String valor) {
        try {
            Object valorEnvio;
            try {
                valorEnvio = Double.parseDouble(valor);
            } catch (NumberFormatException ex) {
                valorEnvio = valor;
            }
            JSONObject setReq = new JSONObject()
                    .put("cmd", "set_req")
                    .put("locate", dispositivo)
                    .put("value", valorEnvio);
            JSONObject resp = enviar(setReq);
            if (resp.has("error")) {
                mostrarErro(resp.getString("error"));
            }
        } catch (Exception e) {
            mostrarErro("Erro ao alterar atuador: " + e.getMessage());
        }
    }

    //Envia shutdown para o servidor
    private void desligarServidor() {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] data = "shutdown".getBytes();
            InetAddress address = InetAddress.getByName(SERVIDOR);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, PORTA);
            socket.send(packet);
            socket.close();
            JOptionPane.showMessageDialog(this, "Comando de shutdown enviado ao servidor!");
        } catch (Exception e) {
            mostrarErro("Erro ao enviar comando: " + e.getMessage());
        }
        dispose();
    }

    //Função para enviar requisições e receber respostas
    private JSONObject enviar(JSONObject req) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        byte[] data = req.toString().getBytes();
        InetAddress address = InetAddress.getByName(SERVIDOR);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, PORTA);
        socket.send(packet);

        byte[] buffer = new byte[2048];
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(responsePacket);

        String resp = new String(responsePacket.getData(), 0, responsePacket.getLength());
        socket.close();
        return new JSONObject(resp);
    }

    //Exibe mensagem de erro
    private void mostrarErro(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}