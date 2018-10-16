/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unipi.studenti.automatedreservationmanager.managedbeans;

import it.unipi.studenti.automatedreservationmanager.singletonbeans.TablesManager;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.Dependent;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 *
 * @author Lorenzo
 */
@ManagedBean (name = "leaveView")
@SessionScoped
public class LeaveView implements Serializable{
    
    @EJB
    private TablesManager tablesManager;
    
    private String ticket;

    /**
     * Creates a new instance of LeaveView
     */
    public LeaveView() {
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }
    
    public String leaveTable(){
        System.out.println("Trying to leave table " + ticket);
        tablesManager.leaveTable(ticket);
        this.ticket = "";
        return "index?faces-redirect=true";
    }
    
    
    
}
