package clientServerArquivos;

import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Base64;

public class Client {
	private static final int PORTA = 4000;
	private static final String HOST = "localhost";
		
	public static void main(String args[]) {
		try (
				//Tenta conectar ao servidor e configura leitura e escrita
				Socket socket = new Socket(HOST, PORTA);
				BufferedReader entradaUsuario = new BufferedReader(new InputStreamReader(System.in));
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			){
				System.out.println("Conectado ao servidor. Digite comandos: (LIST, GET <file>, PUT <file>, QUIT)");
				while (true) {
					//Lê comandos do usuario e ignora caso esteja vazio
	                System.out.print("ftp> ");
	                String line = entradaUsuario.readLine();
	                if (line == null || line.trim().isEmpty()) continue;

	                //Divide em partes o cmd e extrai a primeira palavra
	                String[] parts = line.trim().split(" ");
	                String cmd = parts[0].toLowerCase();
	                JSONObject request = new JSONObject();
	                
	                switch(cmd) {
	                case "list":
	                	//Monta um JSON com o comando
	                	request.put("cmd", "list_req");
	                	break;
	                case "put": 
	                	if (parts.length != 2) {
                            System.out.println("Uso: PUT <nome_arquivo>");
                            continue;
                        }
	                	String fileName = parts[1];
                        File file = new File(fileName);

                        if (!file.exists()) {
                            System.out.println("Arquivo não encontrado localmente.");
                            continue;
                        }

                        //Obtem o conteudo, codifica em base64 e calcula o hash
                        byte[] content = Files.readAllBytes(Paths.get(fileName));
                        String base64Value = Base64.getEncoder().encodeToString(content);
                        String hash = md5Hash(content);

                        //Monta o JSON
                        request.put("cmd", "put_req");
                        request.put("file", fileName);
                        request.put("value", base64Value);
                        request.put("hash", hash);
                        break;
	                case "get":
	                	//Verifica se o nome foi informado e envia a requisição
	                	if (parts.length != 2) {
                            System.out.println("Uso: GET <nome_arquivo>");
                            continue;
                        }
                        request.put("cmd", "get_req");
                        request.put("file", parts[1]);
                        break;
	                case "quit": 
	                	//Encerra o cliente
	                	 request.put("cmd", "quit");
	                     out.write(request.toString() + "\n");
	                     out.flush();
	                     System.out.println("Desconectado.");
	                     return;
	                default:
	                	System.out.println("Comando inválido.");
                        continue;
	                }
	                //Envia a requisição
	                out.write(request.toString() + "\n");
	                out.flush();
	                
	                //Recebe a resposta
	                String responseStr = in.readLine();
	                if (responseStr == null) {
	                    System.out.println("Servidor encerrou a conexão.");
	                    break;
	                }
	                JSONObject response = new JSONObject(responseStr);

	                //Trata a resposta de acordo com o tipo
	                switch (response.getString("cmd")) {
	                    case "list_resp":
	                    	//Exibe a lista de arquivos do servidor
	                        System.out.println("Arquivos disponíveis:");
	                        for (Object fileItem : response.getJSONArray("files")) {
	                            System.out.println("- " + fileItem);
	                        }
	                        break;

	                    case "put_resp":
	                    	//Exibe o status da operação PUT
	                        System.out.println("Upload do arquivo " + response.getString("file") + ": " + response.getString("status"));
	                        break;

	                    case "get_resp":
	                    	//Decodifica o conteudo do arquivo
	                        String receivedFile = response.getString("file");
	                        byte[] fileData = Base64.getDecoder().decode(response.getString("value"));
	                        String receivedHash = md5Hash(fileData);

	                        //Valida o hash
	                        if (!receivedHash.equals(response.getString("hash"))) {
	                            System.out.println("Hash incorreto! Arquivo pode estar corrompido.");
	                        } else {
	                            Files.write(Paths.get(receivedFile), fileData);
	                            System.out.println("Arquivo '" + receivedFile + "' salvo com sucesso.");
	                        }
	                        break;

	                    case "error":
	                    	//Caso servidor envia mensagem de erro
	                        System.out.println("Erro: " + response.optString("message", "Erro desconhecido"));
	                        break;
	                    default:
	                    	//Caso servidor envie algo fora do esperado
	                        System.out.println("Resposta desconhecida: " + response);
	                        break;
	                }	                	               
	            }

		}catch(Exception e) {
			System.out.println("Erro no cliente:");
			e.printStackTrace();
		}		
		System.out.println("Cliente encerrado.");	
	}
	
	//Função que calcula o hash de um array de bytes
    private static String md5Hash(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
