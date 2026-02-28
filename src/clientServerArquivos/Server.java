package clientServerArquivos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

public class Server {
	private static final int PORTA = 4000;
	private static final String BASE_DIR = "arquivos_sistema";
	
	public static void main(String args[]) {
		new File(BASE_DIR).mkdirs();
		//Tenta criar um socket
		try (ServerSocket socketServer = new ServerSocket(PORTA)){
			System.out.println("Servidor iniciado na porta " + PORTA);
			
			//Faz o looping que espera as conexões dos clientes
			while(true) {
				Socket socketCliente = socketServer.accept();
	            System.out.println("Cliente conectado: " + socketCliente.getInetAddress());
	            new Thread(() -> tratarCliente(socketCliente)).start();			}				
		} catch (IOException e) {
			System.out.println("Erro ao iniciar servidor" + e.getMessage()); 
		}
	}
	
	public static void tratarCliente(Socket socket) {
		//Configura entrada e saida
		try(BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		){
			String inputLine;
			//Loop para processar todas as mensagens do cliente enquanto ele estiver conectado
			 while ((inputLine = in.readLine()) != null) {
				 	//Converte a linha recebida em um objeto JSON
	                JSONObject request = new JSONObject(inputLine);                
	                JSONObject response = new JSONObject();
	                String cmd = request.optString("cmd");
	                
	                //Interpreta o comando solicitado pelo cliente
	                switch(cmd) {
	                //Requisicao da lista
	                case "list_req": 
	                	File dir = new File(BASE_DIR);
	                	String[] arquivos = dir.list();
	                	JSONArray arrayArquivos = new JSONArray(arquivos != null ? arquivos : new String[0]);
	                	response.put("cmd", "list_resp");
                        response.put("files", arrayArquivos);
	                	break;
	                //Requisicao de um arquivo
	                case "get_req":
	                	String arquivoAObter = request.getString("file");
                        Path caminhoArquivo = Paths.get(BASE_DIR, arquivoAObter);
                        
                        if (Files.exists(caminhoArquivo)) {
                        	//Lê o conteúdo do arquivo em bytes e codifica em Base64
                            byte[] conteudo = Files.readAllBytes(caminhoArquivo);
                            String base64 = Base64.getEncoder().encodeToString(conteudo);
                            String hash = md5Hash(conteudo);
                            
                            response.put("cmd", "get_resp");
                            response.put("file", arquivoAObter);
                            response.put("value", base64);
                            response.put("hash", hash);
                        } else {
                        	 //Nao encontrado
                        	 response.put("cmd", "error");
                             response.put("message", "Arquivo não encontrado");
                        }
                        break;
                    //Requisicao de colocar um arquivo
	                case "put_req":
	                	String arquivoAColocar = request.optString("file");
	                	String dadosBase64 = request.optString("value");
	                	String receivedHash = request.optString("hash");
	                	
	                	//Decodifica os dados recebidos de Base64 para bytes
	                	byte[] bytesDecodificados = Base64.getDecoder().decode(dadosBase64);
                        String hashCalculado = md5Hash(bytesDecodificados);
	                	            
                        //Se o hash recebido for diferente do calculado, rejeita
                        if (!receivedHash.equals(hashCalculado)) {
                            response.put("cmd", "put_resp");
                            response.put("file", arquivoAColocar);
                            response.put("status", "fail");
                        //Salva o arquivo no diretório base
                        } else {
                            Path caminhoAColocar = Paths.get(BASE_DIR, arquivoAColocar);
                            Files.write(caminhoAColocar, bytesDecodificados);
                            response.put("cmd", "put_resp");
                            response.put("file", arquivoAColocar);
                            response.put("status", "ok");
                        }
                        break;       
                    //Sair
	                case "quit":
	                	//Fecha o socket
	                	response.put("cmd", "quit_resp");
	                    out.write(response.toString() + "\n");
	                    out.flush();
	                    System.out.println("Cliente solicitou encerramento.");
	                    return;
                    default: 
                    	//Exibe mensagem de erro
                    	response.put("cmd", "error");
                        response.put("message", "Comando inválido");
                        break;
	                }
	                //Envia a resposta para o cliente
	                out.write(response.toString() + "\n");
	                out.flush();
			 }
			
		}catch (Exception e) {
            e.printStackTrace();
		}
	}
		
		//Função para gerar o hash MD5
	    private static String md5Hash(byte[] data) throws Exception {
	        MessageDigest md = MessageDigest.getInstance("MD5");
	        byte[] digest = md.digest(data);
	        StringBuilder sb = new StringBuilder();
	        for (byte b : digest) sb.append(String.format("%02x", b));
	        return sb.toString();
	    }		
}