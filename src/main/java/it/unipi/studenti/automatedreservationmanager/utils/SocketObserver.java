/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unipi.studenti.automatedreservationmanager.utils;

import it.unipi.studenti.automatedreservationmanager.singletonbeans.TablesManager;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.websocket.CloseReason;
import org.omnifaces.cdi.push.SocketEvent;

/**
 *
 * @author Lorenzo
 */
@ApplicationScoped
public class SocketObserver {
    
    @EJB
    private TablesManager tmBean;

    public void onOpen(@Observes @SocketEvent.Opened SocketEvent event) {
        String channel = event.getChannel(); // Returns <o:socket channel>.
        Long userId = event.getUser(); // Returns <o:socket user>, if any.
        // Do your thing with it. E.g. collecting them in a concurrent/synchronized collection.
        // Do note that a single person can open multiple sockets on same channel/user.
    }

    public void onClose(@Observes @SocketEvent.Closed SocketEvent event) {
        String channel = event.getChannel(); // Returns <o:socket channel>.
        Long userId = event.getUser(); // Returns <o:socket user>, if any.
        CloseReason.CloseCode code = event.getCloseCode(); // Returns close reason code.
        // Do your thing with it. E.g. removing them from collection.
        
        System.out.println("Socket closed: " + channel + " " + userId + " " + code);
        tmBean.removeNormalRequestByUserId(userId);
    }

}
