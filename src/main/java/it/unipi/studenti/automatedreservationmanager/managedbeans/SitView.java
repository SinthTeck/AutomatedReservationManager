/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unipi.studenti.automatedreservationmanager.managedbeans;

import it.unipi.studenti.automatedreservationmanager.model.RealTimeRequest;
import it.unipi.studenti.automatedreservationmanager.singletonbeans.TablesManager;
import it.unipi.studenti.automatedreservationmanager.singletonbeans.UserIdGenerator;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

/**
 *
 * @author Lorenzo
 */
@ManagedBean (name = "sitView")
@SessionScoped
public class SitView implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int seats = 1;
    private long userId;
    private String ticket;
    
    @ManagedProperty(value = "#{leaveView}")
    private LeaveView leaveView;
    
    

    @Inject
    @Push
    private PushContext mChannel;

    @EJB
    private TablesManager tablesManager;

    @EJB
    private UserIdGenerator userIdGenerator;

    public SitView() {
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public LeaveView getLeaveView() {
        return leaveView;
    }

    public void setLeaveView(LeaveView leaveView) {
        this.leaveView = leaveView;
    }
    
    
    
    public void attemptSit() {
//        System.out.println(mChannel);
        RealTimeRequest rtr = new RealTimeRequest(seats, mChannel, userId);
        tablesManager.addNormalRequest(rtr);
    }

    public void setUserId(long userId) {
    }

    public long getUserId() {

        FacesContext fCtx = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(false);
        String sessionId = session.getId();

        userId = userIdGenerator.getUserId(sessionId);
        return userId;
    }
    
    public String redirectToLeave(){
        this.ticket = tablesManager.getTicketFromUserId(this.userId);
        leaveView.setTicket(ticket);
        
        return "leave";
    }

}
