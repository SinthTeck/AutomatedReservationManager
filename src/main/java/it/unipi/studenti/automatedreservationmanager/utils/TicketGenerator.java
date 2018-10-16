/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unipi.studenti.automatedreservationmanager.utils;

import java.util.Random;

/**
 *
 * @author Lorenzo
 */
public class TicketGenerator {

    public static String getRandomTicket() {
        String ticket = "";
        
        Random r = new Random();
        String alphabet = "123456789qwertyuiopasdfghjklzxcvbnm";
        
        for (int i = 0; i < 5; i++) {
            ticket += alphabet.charAt(r.nextInt(alphabet.length()));
        }
        return ticket;
    }
}
