/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.tudd.homework2.hangman;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 *
 * @author udde
 */
public class SessionHandler {
    
    private final int MAX_ACTION_LENGTH = 128;
    private boolean isConnected;
    private Hangman hangmanGame;
    SocketChannel clientChannel;
    private final ByteBuffer playerAction = ByteBuffer.allocateDirect(MAX_ACTION_LENGTH);
    final Queue<ByteBuffer> responsesToSend = new ArrayDeque<>();
    private Selector selector;
    boolean hasDisconnected = false;
    
    private final String START = "#Start";
    private final String QUIT = "#Quit";

    public SessionHandler(Selector selector, SocketChannel clientChannel) {
        hangmanGame = new Hangman();
        this.clientChannel = clientChannel;
        this.selector = selector;
    }
    
    public void handle() throws IOException {
        String action = readMessage();
        switch(action){
            case START:
                prepareResponse(hangmanGame.start());
                selector.wakeup();
                break;
            case QUIT:
                clientChannel.close();
                hasDisconnected = true;
            default:
                prepareResponse(hangmanGame.guess(action));
                selector.wakeup();
                break;
        }
    }
    
    private void prepareResponse(String response){
        synchronized (responsesToSend) {
            responsesToSend.add(ByteBuffer.wrap(response.getBytes()));
        }
    }
    
    private String readMessage() throws IOException{
        playerAction.clear();
        int numOfReadBytes;
        numOfReadBytes = clientChannel.read(playerAction);
        if (numOfReadBytes == -1) {
            throw new IOException("Client has closed connection.");
        }
        playerAction.flip();
        byte[] bytes = new byte[playerAction.remaining()];
        playerAction.get(bytes);
        return new String(bytes);
        
    }
    
}
