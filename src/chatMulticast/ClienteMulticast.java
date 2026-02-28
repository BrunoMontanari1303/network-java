package chatMulticast;

import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;
import java.util.Scanner;

public class ClienteMulticast {
    private static final String ENDERECO_MULTICAST = "230.0.0.0";
    private static final int PORTA = 12345;

    public static void main(String[] args) throws IOException, InterruptedException {
    	//Cria o socket para participar de multicast na porta 12345
        MulticastSocket socket = new MulticastSocket(PORTA);
        //Obtem o endereço do grupo multicast
        InetAddress group = InetAddress.getByName(ENDERECO_MULTICAST);
        //socket criado entra no grupo
        socket.joinGroup(group);
        
        Scanner leitor = new Scanner(System.in);
        System.out.print("Digite seu nome: ");
        String nome = leitor.nextLine();

        //Thread para receber mensagens
        Thread receiveThread = new Thread(() -> {
        	
            byte[] buffer = new byte[1024];
            
            try {
            	//Enquanto a thread continuar
                while (true) {
                	
                	//Aguarda recebimento dos pacotes
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    //Converte os bytes recebidos em string e cria o JSON
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    JSONObject json = new JSONObject(msg);
                    
                    //Imprime as informaçoes do JSON
                    System.out.print("\r");
                    System.out.println("[" + json.getString("date") + " " + json.getString("time") + "] " + json.getString("username") + ": " + json.getString("message"));

                }
            } catch (SocketException e) {
                //Socket foi fechado, encerrar a thread
                System.out.println("Recepção encerrada.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        
        //Inicia a thread
        receiveThread.start();
        
        //Enviar mensagens do console
        System.out.println("Digite sua mensagem (para sair digite '/sair'):");
        
        Thread sendThread = new Thread(() -> {
        	//Loop de envio de mensagens
            while (true) {
            	String mensagem = leitor.nextLine();
            	System.out.println();
                
            	//Comando de sair
                if (mensagem.equalsIgnoreCase("/sair")) {
                    System.out.println("Saindo do chat...");
                    break;
                }

                //Monta o JSON com os dados da mensagem
                JSONObject json = new JSONObject();
                json.put("date", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                json.put("time", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                json.put("username", nome);
                json.put("message", mensagem);

                //Converte o JSON em bytes e manda para o grupo
                byte[] data = json.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, group, PORTA);
                try {
					socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
            }       	        	
        });
        sendThread.start();
        sendThread.join();
        socket.leaveGroup(group);
        socket.close();
        receiveThread.interrupt();        		        
        leitor.close();
    }
}