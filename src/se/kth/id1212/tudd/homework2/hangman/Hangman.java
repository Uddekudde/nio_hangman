/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.tudd.homework2.hangman;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import static java.lang.Math.random;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author udde
 */
public class Hangman {
    private boolean playing = false;
    private boolean activeGame;
    private int score = 0;
    private int attempts;
    private int unguessedLetters;
    private String word;
    private char[] letters;
    private ArrayList<String> words;
    
    public String start(){
        if(activeGame){
            return "You already have an active game. "+new String(letters)+" "
                    + "You have "+attempts+" attempts left. Your score is "+score+".";
        }
        if(playing){
            activeGame = true;
            newWord(words);
            return "Game started. "+new String(letters)+" You have "+attempts+
                    " attempts left. Your score is "+score+".";
        }
        try {
            BufferedReader fromFile = new BufferedReader(new FileReader("words.txt"));
            words = new ArrayList<>();
            String str;
            while((str = fromFile.readLine()) != null){
                words.add(str);
            }
            newWord(words);
            activeGame = true;
            playing = true;
            return "Game started. "+new String(letters)+" You have "+attempts+
                    " attempts left. Your score is "+score+".";
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Hangman.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Hangman.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "Something went wrong";
    }
    public void newWord(ArrayList<String> words){
        Random random = new Random();
        int index = random.nextInt(words.size());
            word = words.get(index).toLowerCase();
            
            letters = new char[word.length()];
            for(int i = 0; i < letters.length; i++){
                letters[i] = '-'; 
            }
            attempts = word.length();
            unguessedLetters = word.length();
    }
    
    public String guess(String guess){
        if(!activeGame){
            return "type #Start to start a game.";
        }
        
        if(guess.length() == 0){
            return "";
        }
  
        if(guess.length() > 1){
            if(guess.equalsIgnoreCase(word)){
                win();
                return "Correct! The word was:"+word+". you have "+score+" points.";
            }
            attempts--;
            if(attempts == 0){
                lose();
                return "Game over! The word was: "+word+".";
            }
            return "Incorrect guess. You have "+attempts+" attempts left. Your "
                    + "score is "+score+".";
        }
        
        if(word.contains(guess.toLowerCase())){
            for(int i = 0; i < word.length(); i++){
                Character letter = word.charAt(i);
                if(letter.compareTo(guess.charAt(0)) == 0){
                    letters[i] = letter;
                    unguessedLetters--;
                }
            }
            if(unguessedLetters == 0){
                win();
                return "Correct! The word was:"+word+". you have "+score+" points.";
            }
            return new String(letters)+" You have "+attempts+" attempts left. "
                    + "Your score is "+score+".";
        }
        attempts--;
        if(attempts == 0){
            lose();
            return "Game over! The word was: "+word+".";
        }
        return "Incorrect guess. You have "+attempts+" attempts left. "
                + "Your score is "+score+".";
    }
    
    public void win(){
        score++;
        activeGame = false;
    }
    
    public void lose(){
        score--;
        activeGame = false;
    }
}
