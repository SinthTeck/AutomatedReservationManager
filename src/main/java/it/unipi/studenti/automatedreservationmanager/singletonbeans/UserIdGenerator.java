/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unipi.studenti.automatedreservationmanager.singletonbeans;

import java.util.HashMap;
import java.util.Map;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

/**
 *
 * @author Lorenzo
 */
@Singleton
@Lock(LockType.READ)
public class UserIdGenerator {

    private Long counter = 0L;
    
    private final Map sessionIdMap = new HashMap();
    
    @Lock(LockType.WRITE)
    public Long getUserId(String sessionId){
        long max = 9223372036854775800L;
        if(!sessionIdMap.containsKey(sessionId)){
            sessionIdMap.put(sessionId, (this.counter++)%max);
        }
        return (Long)sessionIdMap.get(sessionId);
    }
    
}
