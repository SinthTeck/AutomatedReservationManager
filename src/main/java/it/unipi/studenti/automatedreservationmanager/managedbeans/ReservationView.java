/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unipi.studenti.automatedreservationmanager.managedbeans;

import it.unipi.studenti.automatedreservationmanager.singletonbeans.DBManagerBean;
import it.unipi.studenti.automatedreservationmanager.singletonbeans.TablesManager;

import java.io.Serializable;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalField;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.sound.midi.SysexMessage;

/**
 *
 * @author Lorenzo
 */
@ManagedBean(name = "reservationView")
@SessionScoped
public class ReservationView implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private DBManagerBean dbBean;

    @EJB
    private TablesManager tablesManager;

    @ManagedProperty(value = "#{leaveView}")
    private LeaveView leaveView;

    private int seats = 1;
    private Date date = new Date();
    private Date time = Time.valueOf(LocalTime.now().plusHours(3));

    private String ticket;
    private Integer tableId;

    private boolean renderMessage = false;
    private String message = "";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    

    public boolean isRenderMessage() {
        return renderMessage;
    }

    public void setRenderMessage(boolean renderMessage) {
        this.renderMessage = renderMessage;
    }

    public Integer getTableId() {
        return tableId;
    }

    public void setTableId(Integer tableId) {
        this.tableId = tableId;
    }

    public LeaveView getLeaveView() {
        return leaveView;
    }

    public void setLeaveView(LeaveView leaveView) {
        this.leaveView = leaveView;
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

    public String makeReservation() {
        
        this.message = "";
        System.out.println(this.time.toString());
        System.out.println(this.date.toString());
        Long mill = this.date.getTime()+this.time.getTime()+3600000;
        Date resTime = new Date(mill);
        System.out.println(resTime);
        
        System.out.println("Today: " + new Date().getTime());
        System.out.println("Res date/time: " + resTime.getTime());
        
        if( new Date().after(resTime)){
            this.renderMessage = true;
            message = "You can't make a reservation in the past.";
            return "";
        }
        if((resTime.getTime() - new Date().getTime() ) < 10800000){
            this.renderMessage = true;
            message = "The reservation has to be done al least three hours earlier.";
            return "";
        }
        
        this.ticket = dbBean.insertReservation(this.seats, this.date, this.time);
        if (this.ticket == null) {
            this.renderMessage = true;
            this.message = "Not enough seats to satisfy you request. Try with a different date or time.";
            return "";
        } else {
            return "reservation_info.xhtml";
        }
    }

    public String useReservation() {
        this.tableId = tablesManager.serveReservation(ticket);
        System.out.println("Table id is: " + tableId);
        if (tableId != null) {
            return "reserved_table_info";
        } else {
            return "";
        }
    }

    public String redirectToLeave() {
        leaveView.setTicket(ticket);
        this.ticket = "";

        return "leave";
    }

    public ReservationView() {
    }

}
