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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 *
 * @author udde
 */
public class HangmanServer {
    
    private final int portNum = 3000;
    private final int LINGER_TIME = 10000;
    private Selector selector;
    private ServerSocketChannel listeningChannel;

    
    private void acceptPlayers(){
        
        try {
            selector = Selector.open();
            listeningChannel = ServerSocketChannel.open();
            listeningChannel.configureBlocking(false);
            listeningChannel.bind(new InetSocketAddress(portNum));
            listeningChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            while(true){
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (!key.isValid()) {
                        continue;
                    }
                if (key.isAcceptable()) {
                        startHandler(key);
                    } else if (key.isReadable()) {
                        recieveMessage(key);
                    } else if (key.isWritable()) {
                        sendResponse(key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error.");
        }
    }
    
    private void recieveMessage(SelectionKey key){
        Executor pool = ForkJoinPool.commonPool();
        SessionHandler handler = (SessionHandler) key.attachment();
        key.interestOps(SelectionKey.OP_CONNECT);
            pool.execute(() -> {
            try {
                handler.handle();
                key.interestOps(SelectionKey.OP_WRITE);
                if(handler.hasDisconnected){
                key.cancel();
                }
            } catch (IOException ex) {
                System.out.println("Player has quit.");
            }
        });
    }
    
    private void sendResponse(SelectionKey key) throws IOException{
        SessionHandler handler = (SessionHandler)key.attachment();
        ByteBuffer msg;
        synchronized (handler.responsesToSend) {
            while ((msg = handler.responsesToSend.peek()) != null) {
                handler.clientChannel.write(msg);
                handler.responsesToSend.remove();
            }
            key.interestOps(SelectionKey.OP_READ);
        }
    }
    
    private void startHandler(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, new SessionHandler(selector, clientChannel));
    }
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        HangmanServer hangman = new HangmanServer();
        hangman.acceptPlayers();
    }
    
}
