/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unipi.studenti.automatedreservationmanager.model;

import java.util.Date;
import org.omnifaces.cdi.PushContext;

/**
 *
 * @author Lorenzo
 */
public class RealTimeRequest {
    private int seats;
    private PushContext context;
    private Long userId;
    private String ticket;
    
    private Date sittingTime;
    
    public RealTimeRequest(int seats, PushContext context, Long userId){
        this.seats = seats;
        this.context = context;
        this.userId = userId;
        
        this.ticket = null;
    }

    public Date getSittingTime() {
        return sittingTime;
    }

    public void setSittingTime(Date sittingTime) {
        this.sittingTime = sittingTime;
    }
    
    

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }
    
    

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public PushContext getContext() {
        return context;
    }

    public void setContext(PushContext context) {
        this.context = context;
    }
    
    
}
