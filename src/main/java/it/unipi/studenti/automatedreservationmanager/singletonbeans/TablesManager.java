/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unipi.studenti.automatedreservationmanager.singletonbeans;

import it.unipi.studenti.automatedreservationmanager.model.RealTimeRequest;
import it.unipi.studenti.automatedreservationmanager.model.Reservation;
import it.unipi.studenti.automatedreservationmanager.model.TableUnit;
import it.unipi.studenti.automatedreservationmanager.utils.Properties;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 *
 * @author Lorenzo
 */
@Startup
@Singleton
@Lock(LockType.READ)
public class TablesManager {

    @EJB
    private DBManagerBean dbBean;

    //RealTime requests queues
    private List<RealTimeRequest> normalRequestsList = new ArrayList<>();
    private List<RealTimeRequest> servedRequestList = new ArrayList<>();

    //Reservation requests queues
    private Queue<Reservation> reservationsList = new ArrayDeque<>();
    private List<Reservation> readyReservationsList = new ArrayList<>();
    private List<Reservation> servedReservationsList = new ArrayList<>();

    //Internal state
    private Queue<Integer> availableTableIds = new LinkedList<>();
    private Queue<Integer> usedTableIds = new LinkedList<>();

    private List<TableUnit> tableUnits = new ArrayList<>();
    private int numberOfAvailableTableUnits;

    public TablesManager() {
    }

    @PostConstruct
    private void init() {
        if (dbBean.hasBackUpData()) {
            System.out.println("Getting backed up data");
            this.normalRequestsList = dbBean.getStoredNormalRequestsList();
            this.reservationsList = dbBean.getStoredReservationsList();
            this.readyReservationsList = dbBean.getStoredReadyReservationsList();
            this.servedRequestList = dbBean.getStoredServedRequestsList();
            this.servedReservationsList = dbBean.getStoredServedReservationsList();
            this.availableTableIds = dbBean.getStoredAvailableTableIds();
            this.usedTableIds = dbBean.getStoredUsedTableIds();
            this.numberOfAvailableTableUnits = dbBean.getStoredNumberOfAvailableTableUnits();
            this.tableUnits = dbBean.getStoredTableUnits();
        } else {
            System.out.println("Normal initialization");
            //Initialize the tableUnits list
            for (int i = 0; i < Properties.NUMBER_OF_TABLES; i++) {
                TableUnit tu = new TableUnit();
                tu.setId(i);
                tu.setTableId(0);
                tu.setFree(true);
                tu.setTicket("");
                tableUnits.add(tu);
                availableTableIds.add(i + 1);
                
                dbBean.storeTableUnit(tu);
                dbBean.storeAvailableTableId(i+1);
            }
            this.numberOfAvailableTableUnits = Properties.NUMBER_OF_TABLES;
            dbBean.storeNumberOfAvailableTableUnits(numberOfAvailableTableUnits);
        }
    }

    @Lock(LockType.WRITE)
    public void addNormalRequest(RealTimeRequest req) {
        for (RealTimeRequest r : this.normalRequestsList) {
            if (r.getUserId().compareTo(req.getUserId()) == 0) {
                //The same user can't make two requests
                return;
            }
        }
        this.normalRequestsList.add(req);
        dbBean.storeNormalRequest(req);
        System.out.println("Added request with seats " + req.getSeats());
        Collections.sort(this.normalRequestsList, new Comparator<RealTimeRequest>() {
            @Override
            public int compare(final RealTimeRequest lhs, RealTimeRequest rhs) {
                //TODO return 1 if rhs should be before lhs 
                //     return -1 if lhs should be before rhs
                //     return 0 otherwise
                if (rhs.getSeats() > lhs.getSeats()) {
                    return 1;
                } else if (rhs.getSeats() < lhs.getSeats()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        this.check();
    }

    @Lock(LockType.WRITE)
    public void removeNormalRequestByUserId(Long userId) {
        Iterator<RealTimeRequest> iter = this.normalRequestsList.iterator();
        while (iter.hasNext()) {
            RealTimeRequest r = iter.next();
            if (r.getUserId().equals(userId)) {
                iter.remove();
                dbBean.removeNormalRequest(r);
            }
        }
    }

    @Lock(LockType.WRITE)
    public void leaveTable(String ticket) {
        boolean found = false;
        //Check the case in which the ticket is from a table WITHOUT reservetion
        Iterator<RealTimeRequest> iterReq = servedRequestList.iterator();
        while (iterReq.hasNext()) {
            RealTimeRequest r = iterReq.next();
            System.out.println("Checking if " + r.getTicket() + " is equal to " + ticket);
            if (r.getTicket().equals(ticket)) {
                found = true;
                iterReq.remove();
                System.out.println("Removed normal table");
                dbBean.removeServedRequest(r);
            }
        }
        //Check the case in which the ticket is from a table WITH reservation
        Iterator<Reservation> iterRes = servedReservationsList.iterator();
        while (iterRes.hasNext()) {
            Reservation r = iterRes.next();
            System.out.println("Checking if " + r.getTicket() + " is equal to " + ticket);
            if (r.getTicket().equals(ticket)) {
                found = true;
                iterRes.remove();
                System.out.println("Removed reserved table");
                dbBean.removeServedReservation(r);
            }
        }

        if (found) {
            Iterator<TableUnit> iterTable = tableUnits.iterator();
            int tableId = 0;
            while (iterTable.hasNext()) {
                TableUnit t = iterTable.next();
                if (t.getTicket().equals(ticket)) {
                    t.setTicket("");
                    t.setFree(true);
                    this.usedTableIds.remove(t.getTableId());
                    this.availableTableIds.add(t.getTableId());
                    tableId = t.getTableId();
                    this.numberOfAvailableTableUnits++;
                    t.setTableId(0);
                    dbBean.storeTableUnit(t);
                    dbBean.storeNumberOfAvailableTableUnits(numberOfAvailableTableUnits);
                }
            }
            dbBean.removeUsedTableId(tableId);
            dbBean.storeAvailableTableId(tableId);
            dbBean.removeTicket(ticket);
        }
        this.check();
    }

    @Lock(LockType.READ)
    public String getTicketFromUserId(Long userId) {
        String ticket = null;
        for (RealTimeRequest r : servedRequestList) {
            if (r.getUserId().compareTo(userId) == 0) {
                return r.getTicket();
            }
        }
        return ticket;
    }

    @Lock(LockType.WRITE)
    private void check() {
//        try{
//        Thread.sleep(3000);
//        }catch(Exception e){}
        //Main logic of the TablesManager
        if (!this.reservationsList.isEmpty()) {
            //Assign free tables to reservations
            while (!this.reservationsList.isEmpty() && this.numberOfAvailableTableUnits != 0) {
                if (this.reservationsList.peek().getTableId() == 0) {
                    this.reservationsList.peek().setTableId(this.availableTableIds.poll());
                    dbBean.removeAvailableTableId(this.reservationsList.peek().getTableId());
                    this.usedTableIds.add(this.reservationsList.peek().getTableId());
                    dbBean.storeUsedTableId(this.reservationsList.peek().getTableId());
                }
                for (TableUnit t : this.tableUnits) {
                    if (t.isFree()) {
                        t.setFree(false);
                        t.setTableId(this.reservationsList.peek().getTableId());
                        t.setTicket(this.reservationsList.peek().getTicket());
                        dbBean.storeTableUnit(t);
                        break;
                    }
                }
                this.numberOfAvailableTableUnits--;
                dbBean.storeNumberOfAvailableTableUnits(this.numberOfAvailableTableUnits);
                this.reservationsList.peek().setSeatsTmp((this.reservationsList.peek().getSeatsTmp() <= 2) ? 0 : (this.reservationsList.peek().getSeatsTmp() - 2));
                System.out.println("Assigned a table unit to reservation with ticket: " + this.reservationsList.peek().getTicket());
                if (this.reservationsList.peek().getSeatsTmp() == 0) {
                    System.out.println("Reservation with ticket: " + this.reservationsList.peek().getTicket() + " served.");
                    Reservation r = this.reservationsList.poll();
                    this.readyReservationsList.add(r);
                    dbBean.storeReadyReservation(r);
                    dbBean.removeReservation(r);
                }
            }

        }

        if (this.reservationsList.isEmpty()) {
            Iterator<RealTimeRequest> iter = this.normalRequestsList.iterator();
            while (iter.hasNext()) {
                RealTimeRequest r = iter.next();
                int neededTables = ((r.getSeats() % 2) == 0) ? r.getSeats() / 2 : (r.getSeats() + 1) / 2;
                if (neededTables <= this.numberOfAvailableTableUnits) {
                    //Make the client sit
                    System.out.println("Making the client sit and removing it's request");
                    this.numberOfAvailableTableUnits -= neededTables;
                    dbBean.storeNumberOfAvailableTableUnits(this.numberOfAvailableTableUnits);
                    Integer tableId = this.availableTableIds.poll();
                    dbBean.removeAvailableTableId(tableId);
                    this.usedTableIds.add(tableId);
                    dbBean.storeUsedTableId(tableId);
                    String ticket = dbBean.getValidTicket();
                    for (TableUnit t : this.tableUnits) {
                        if (t.isFree()) {
                            t.setFree(false);
                            t.setTableId(tableId);
                            t.setTicket(ticket);

                            dbBean.storeTableUnit(t);

                            if (--neededTables == 0) {
                                break;
                            }
                        }
                    }
                    r.setTicket(ticket);
                    r.setSittingTime(Time.valueOf(LocalTime.now()));
                    this.servedRequestList.add(r);
                    dbBean.storeServedRequest(r);
                    dbBean.removeNormalRequest(r);
                    JsonObjectBuilder builder = Json.createObjectBuilder();
                    builder.add("ticket", ticket);
                    builder.add("tableId", tableId.toString());
                    JsonObject json = builder.build();
                    r.getContext().send(json);
                    iter.remove();
                }
            }
        }

        for (Reservation r : this.reservationsList) {
            System.out.println("Reservation id: " + r.getTicket() + ", seats: " + r.getSeats());
        }

        for (RealTimeRequest rt : this.normalRequestsList) {
            System.out.println("RealTimeRequest id: " + rt.getUserId() + ", seats: " + rt.getSeats());
        }

    }

    @Lock(LockType.WRITE)
    public Integer serveReservation(String ticket) {
        Iterator<Reservation> iter = this.readyReservationsList.iterator();
        while (iter.hasNext()) {
            Reservation r = iter.next();
            if (r.getTicket().equals(ticket)) {
                this.servedReservationsList.add(r);
                dbBean.storeServedReservation(r);
                dbBean.removeReadyReservation(r);
                r.setSittingTime(Time.valueOf(LocalTime.now()));
                iter.remove();
                return r.getTableId();
            }
        }
        return null;
    }

    @Lock(LockType.WRITE)
    @Schedule(hour = "*", minute = "*", second = "*/10")
    private void checkReservations() {
        java.util.Date today = Date.valueOf(LocalDate.now());
        java.util.Date later = Time.valueOf(LocalTime.now().plusHours(3));

        List<Reservation> res = dbBean.getReservation(today, later);

        for (Reservation r : res) {
            System.out.println(r.getTicket());
            System.out.println(r.getId());
            System.out.println(r.getSeats());
            System.out.println(r.getDate());
            System.out.println(r.getTime());

            this.reservationsList.add(r);
            dbBean.storeReservation(r);
        }

        //Check if a client has been sitting for more than 2 hours
        java.util.Date now = Time.valueOf(LocalTime.now());
        Iterator<RealTimeRequest> iterReq = this.servedRequestList.iterator();
        System.out.println("Requests being served: ");
        while (iterReq.hasNext()) {
            RealTimeRequest r = iterReq.next();
            System.out.println("    Ticket: " + r.getTicket() + ", Time passed: " + (now.getTime() - r.getSittingTime().getTime()) + "ms");
            if (now.getTime() - r.getSittingTime().getTime() > 1000 * 60 * 60 * 2) {
                System.out.println(r.getTicket() + " will have to leave");
                iterReq.remove();
                dbBean.removeServedRequest(r);
                Iterator<TableUnit> iterTable = tableUnits.iterator();
                while (iterTable.hasNext()) {
                    TableUnit t = iterTable.next();
                    if (t.getTicket().equals(r.getTicket())) {
                        t.setTicket("");
                        t.setFree(true);
                        this.usedTableIds.remove(t.getTableId());
                        this.availableTableIds.add(t.getTableId());
                        this.numberOfAvailableTableUnits++;
                        dbBean.storeTableUnit(t);
                    }
                }
                dbBean.storeNumberOfAvailableTableUnits(this.numberOfAvailableTableUnits);
                dbBean.removeTicket(r.getTicket());
            }
        }
        Iterator<Reservation> iterRes = this.servedReservationsList.iterator();
        System.out.println("Reservations being served: ");
        while (iterRes.hasNext()) {
            Reservation r = iterRes.next();
            System.out.println("    Ticket: " + r.getTicket() + ", Time passed: " + (now.getTime() - r.getSittingTime().getTime()) + "ms");

            if ((now.getTime() - r.getSittingTime().getTime()) > 1000 * 60 * 60 * 2) {
                System.out.println(r.getTicket() + " will have to leave");
                iterRes.remove();
                dbBean.removeServedReservation(r);
                Iterator<TableUnit> iterTable = tableUnits.iterator();
                while (iterTable.hasNext()) {
                    TableUnit t = iterTable.next();
                    if (t.getTicket().equals(r.getTicket())) {
                        t.setTicket("");
                        t.setFree(true);
                        this.usedTableIds.remove(t.getTableId());
                        this.availableTableIds.add(t.getTableId());
                        this.numberOfAvailableTableUnits++;
                        dbBean.storeTableUnit(t);
                    }
                }
                dbBean.storeNumberOfAvailableTableUnits(this.numberOfAvailableTableUnits);
                dbBean.removeTicket(r.getTicket());
            }
        }

        Iterator<Reservation> iterWait = this.readyReservationsList.iterator();
        System.out.println("Reservations being served: ");
        while (iterWait.hasNext()) {
            Reservation r = iterWait.next();
            Long mill = r.getDate().getTime() + r.getTime().getTime() + 3600000;
            System.out.println("Diff between curr time and res time: " + (new java.util.Date().getTime() - mill));
            if ((new java.util.Date().getTime() - mill) > 1000 * 60 * 15) {
                System.out.println(r.getTicket() + " will have to leave");
                iterWait.remove();
                dbBean.removeReadyReservation(r);
                Iterator<TableUnit> iterTable = tableUnits.iterator();
                while (iterTable.hasNext()) {
                    TableUnit t = iterTable.next();
                    if (t.getTicket().equals(r.getTicket())) {
                        t.setTicket("");
                        t.setFree(true);
                        this.usedTableIds.remove(t.getTableId());
                        this.availableTableIds.add(t.getTableId());
                        this.numberOfAvailableTableUnits++;
                        dbBean.storeTableUnit(t);
                    }
                }
                dbBean.storeNumberOfAvailableTableUnits(this.numberOfAvailableTableUnits);
                dbBean.removeTicket(r.getTicket());
            }
        }

        this.check();
        System.out.println("Date: " + today + ", Time: " + now);
    }
}
