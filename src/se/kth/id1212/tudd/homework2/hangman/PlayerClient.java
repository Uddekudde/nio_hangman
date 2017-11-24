/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.tudd.homework2.hangman;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Scanner;

/**
 *
 * @author udde
 */
public class PlayerClient implements Runnable {
    final String hangmanServer = "127.0.0.1";
    final int MAX_ACTION_LENGTH = 128;
    final int serverPort = 3000;
    boolean isConnected;
    boolean timeToSend = false;
    
    InetSocketAddress serverAddress;
    SocketChannel socketChannel;
    Selector selector;
    final ByteBuffer msgFromServer = ByteBuffer.allocateDirect(MAX_ACTION_LENGTH);
    private final Queue<ByteBuffer> messagesToSend = new ArrayDeque<>();
    @Override
    public void run() {
        try {
            while(isConnected){
                if(timeToSend){
                    socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                    timeToSend = false;
                }
                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                        selector.selectedKeys().remove(key);
                        if (!key.isValid()) {
                            continue;
                        }
                        if (key.isConnectable()) {
                            finishConnecting(key);
                        } else if (key.isReadable()) {
                            handleResponse();
                        } else if (key.isWritable()) {
                            sendMessage(key);
                        }
                    }
            }
        } catch (IOException ex) {
            System.out.println("Could not connect to server");
        }
    }
    
    private void connect(){
        try {
            serverAddress = new InetSocketAddress(hangmanServer, serverPort);
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(serverAddress);
            isConnected = true;
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
    
    private void finishConnecting(SelectionKey key) throws IOException {
        socketChannel.finishConnect();
        try {
            InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
            System.out.println("connecion successful!");
        } catch (IOException ioe) {
                System.out.println("connection error.");
        }    
    }
    
     private void sendMessage(SelectionKey key) throws IOException {
        ByteBuffer msg;
        synchronized (messagesToSend) {
            while ((msg = messagesToSend.peek()) != null) {
                socketChannel.write(msg);
                if (msg.hasRemaining()) {
                    return;
                }
                messagesToSend.remove();
            }
            key.interestOps(SelectionKey.OP_READ);
        }
        if(!isConnected){
            disconnect();
        }
    }

    
    private void handleResponse()throws IOException{
        msgFromServer.clear();
        int numOfReadBytes = socketChannel.read(msgFromServer);
        if (numOfReadBytes == -1) {
            throw new IOException("Error reading message from server.");
        }
        msgFromServer.flip();
        byte[] bytes = new byte[msgFromServer.remaining()];
        msgFromServer.get(bytes);
        System.out.println(new String(bytes));
    }
    
    private void disconnect(){
        try {
            socketChannel.close();
        } catch (IOException ex) {
            System.out.println("Error on disconnect.");
        }
        socketChannel.keyFor(selector).cancel();
    }
    
    private class Interpreter implements Runnable {
        final String QUIT = "#Quit";
        Scanner input = new Scanner(System.in);

        @Override
        public void run(){
            String userInput;
            while(isConnected){
                switch(userInput = input.nextLine()){
                    case QUIT:
                        synchronized (userInput) {
                            prepareResponse(userInput);
                            isConnected = false;
                            selector.wakeup();
                        }
                        break;
                    default:
                        synchronized (userInput) {
                            prepareResponse(userInput);
                            selector.wakeup();
                        }
                        break;
                }
            }
        }
        
        private void prepareResponse(String userInput){
        synchronized (messagesToSend) {
            messagesToSend.add(ByteBuffer.wrap(userInput.getBytes()));
            timeToSend = true;
        }
    }
    }
    
    public static void main(String[] args) {
    PlayerClient playerClient = new PlayerClient();
     System.out.println("Type #Start to start a game, then type your guess. "
             + "Type #Quit at any time to close the program.");
    playerClient.connect();
    new Thread(playerClient.new Interpreter()).start();
    playerClient.run();
    }
}
