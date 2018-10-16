/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unipi.studenti.automatedreservationmanager.model;

import java.util.Date;

/**
 *
 * @author Lorenzo
 */
public class Reservation {
    private int id;
    private String ticket;
    private int seats;
    private int seatsTmp;
    private Date date;
    private Date time;
    private int tableId;
    private Date sittingTime;

    public Date getSittingTime() {
        return sittingTime;
    }

    public void setSittingTime(Date sittingTime) {
        this.sittingTime = sittingTime;
    }

    public int getSeatsTmp() {
        return seatsTmp;
    }

    public void setSeatsTmp(int seatsTmp) {
        this.seatsTmp = seatsTmp;
    }

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }
    
    

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
    
    
}
