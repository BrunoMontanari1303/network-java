package clientServerControlador;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import org.json.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Server {
    private static final int PORTA = 5000;
    private static final Map<String, Object> dispositivos = new HashMap<>();
    
    public static void main(String[] args) throws Exception {
        
    	//Lê o arquivo JSON externo para configurar os dispositivos
        String jsonStr = new String(Files.readAllBytes(Paths.get("dispositivos.json")));
        JSONObject jsonDispositivos = new JSONObject(jsonStr);

        //Limpa e popula o HashMap 'dispositivos' com os valores do JSON
        dispositivos.clear();
        for (String key : jsonDispositivos.keySet()) {
            dispositivos.put(key, jsonDispositivos.get(key));
        }

        //Cria o socket na porta e cria um buffer para os dados recebidos
        DatagramSocket socket = new DatagramSocket(PORTA);
        byte[] buffer = new byte[1024];
        
        System.out.println("Servidor UDP aguardando na porta " + PORTA);
        
        //Loop principal que escuta continuamente por mensagens dos clientes
        while(true) {
            //Cria o pacote para receber dados
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            //Espera recebimento de pacotes
            socket.receive(packet);
            //Converte os dados recebidos para String
            String recebido = new String(packet.getData(), 0, packet.getLength());
            
            //Caso receba o comando "shutdown", encerra o servidor
            if (recebido.equalsIgnoreCase("shutdown")) {
                System.out.println("Encerrando servidor UDP...");
                socket.close();  // Fecha o socket
                break;           // Sai do laço e encerra o programa
            }
            
            //Transforma a mensagem recebida em um objeto JSON
            JSONObject requisicao = new JSONObject(recebido);

            //Processa o comando e gera a resposta em JSON
            JSONObject resposta = tratarRequest(requisicao);

            //Envia a resposta de volta para o cliente (mesmo endereço e porta de origem)
            byte[] infoResposta = resposta.toString().getBytes();
            DatagramPacket packetResposta = new DatagramPacket(infoResposta, infoResposta.length, packet.getAddress(), packet.getPort());
            socket.send(packetResposta);
        }
    }      
    //Processa os comandos recebidos dos clientes e retorna a resposta em JSON.    
    private static JSONObject tratarRequest(JSONObject req) {
    	//Identifica o comando recebido
        String cmd = req.getString("cmd");    
        JSONObject resposta = new JSONObject();

        switch (cmd) {
            case "list_req":
                //Retorna a lista de todos os dispositivos cadastrados
                resposta.put("cmd", "list_resp");
                resposta.put("place", dispositivos.keySet());
                break;
            case "get_req":
                //Retorna o valor de um ou todos os dispositivos
                resposta.put("cmd", "get_resp");
                String local = req.getString("place");
                if (local.equals("all")) {
                    //Responde com todos os dispositivos e valores
                    resposta.put("place", dispositivos.keySet());
                    resposta.put("value", dispositivos.values());
                } else {
                    //Responde apenas com o valor do dispositivo solicitado
                    resposta.put("place", local);
                    resposta.put("value", dispositivos.get(local));
                }
                break;
            case "set_req":
                //Tenta alterar o valor de um atuador
                String locate = req.getString("locate");
                Object value = req.get("value");
                if (locate.startsWith("actuator")) {
                    //Se for uma luz, so aceita 'on' ou 'off'
                    if (locate.contains("light")) {
                        if (!("on".equalsIgnoreCase(value.toString()) || "off".equalsIgnoreCase(value.toString()))) {
                            resposta.put("cmd", "set_resp");
                            resposta.put("locate", locate);
                            resposta.put("error", "Só é permitido 'on' ou 'off' para luzes.");
                            break;
                         }
                    }
                    //Atualiza o valor do atuador
                    dispositivos.put(locate, value);
                    salvarDispositivos();
                    resposta.put("cmd", "set_resp");
                    resposta.put("locate", locate);
                    resposta.put("value", value);
                } else {
                    //Não permite alteração em sensores
                    resposta.put("cmd", "set_resp");
                    resposta.put("locate", locate);
                    resposta.put("error", "Nao pode alterar o valor de um sensor");
                }
                break;
            default:
                //Comando não reconhecido
                resposta.put("error", "comando invalido");
        }
        return resposta;
    }
    
    private static void salvarDispositivos() {
        try {
            JSONObject jsonAtual = new JSONObject(dispositivos);
            Files.write(Paths.get("dispositivos.json"), jsonAtual.toString(4).getBytes());
        } catch (Exception e) {
            System.out.println("Erro ao salvar dispositivos: " + e.getMessage());
        }
    }
}