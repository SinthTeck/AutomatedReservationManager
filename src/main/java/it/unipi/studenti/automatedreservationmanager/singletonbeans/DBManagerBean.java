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
import it.unipi.studenti.automatedreservationmanager.utils.TicketGenerator;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import javax.ejb.Asynchronous;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

/**
 *
 * @author Lorenzo
 */
@Singleton
@Lock(LockType.READ)
public class DBManagerBean {

    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;

    @Lock(LockType.WRITE)
    public void openConnection() {
        if (connect != null) {
            return;
        }
        try {
            Class.forName("com.mysql.jdbc.Driver");

            connect = DriverManager
                    .getConnection("jdbc:mysql://localhost/tablemanagerdb", "root", "root");

            if (connect != null) {
                System.out.println("Database connesso");
            } else {
                System.err.println("Database non connesso");
            }
        } catch (Exception e) {
        }
    }

    @Lock(LockType.WRITE)
    public void removeTicket(String ticket) {
        try {
            this.openConnection();
            statement = connect.createStatement();
            String query = "DELETE from tablemanagerdb.tickets "
                    + "WHERE ticket=\"" + ticket + "\"";
            statement.execute(query);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Lock(LockType.WRITE)
    public String getValidTicket() {
        this.openConnection();
        String ticket = "";
        boolean keep = true;
        try {
            do {
                ticket = TicketGenerator.getRandomTicket();
                statement = connect.createStatement();
                resultSet = statement.
                        executeQuery("select * from tablemanagerdb.tickets where ticket=\'" + ticket + "\'");
                keep = resultSet.next();
            } while (keep);

            preparedStatement = connect
                    .prepareStatement("insert into tablemanagerdb.tickets "
                            + "values (?)");
            preparedStatement.setString(1, ticket);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ticket;
    }

    @Lock(LockType.WRITE)
    public List<Reservation> getReservation(Date date, Date time) {
        List<Reservation> reservations = new ArrayList<>();
        try {
            this.openConnection();

            java.sql.Date sqlDate = new java.sql.Date(date.getTime());
            java.sql.Time sqlTime = new java.sql.Time(time.getTime());

            statement = connect.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM tablemanagerdb.reservations where date=\'" + sqlDate.toString() + "\' and time=\'" + sqlTime.toString() + "\';");
            preparedStatement = connect.
                    prepareStatement("DELETE FROM tablemanagerdb.reservations where date=\'" + sqlDate.toString() + "\' and time=\'" + sqlTime.toString() + "\';");
            preparedStatement.executeUpdate();
            while (resultSet.next()) {
                Reservation r = new Reservation();
                r.setId(resultSet.getInt("id"));
                r.setTicket(resultSet.getString("ticket"));
                r.setSeats(resultSet.getInt("seats"));
                r.setSeatsTmp(resultSet.getInt("seats"));
                r.setDate(resultSet.getDate("date"));
                Time t = resultSet.getTime("time");
                Date t2 = new Date(t.getTime());
                r.setTime(t2);

//                //TODO: delete the tickets entry with one single sql command
//                preparedStatement = connect.
//                        prepareStatement("DELETE FROM tablemanagerdb.tickets where ticket=\'" + r.getTicket() + "\';");
//                preparedStatement.executeUpdate();
                reservations.add(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return reservations;
    }

    @Lock(LockType.WRITE)
    public String insertReservation(int seats, Date date, Date time) {
        System.out.println("Entered");
        String ticket = null;

        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
        java.sql.Time sqlTime = new java.sql.Time(time.getTime());

        try {
            this.openConnection();
            //Generate valid ticket
            ticket = this.getValidTicket();

            if (checkBackward(connect, seats, sqlDate, sqlTime) && checkForward(connect, seats, sqlDate, sqlTime)) {
                preparedStatement = connect
                        .prepareStatement("insert into tablemanagerdb.reservations "
                                + "values (0, ?, ?, ?, ?)");
                preparedStatement.setString(1, ticket);
                preparedStatement.setInt(2, seats);
                preparedStatement.setDate(3, sqlDate);
                preparedStatement.setTime(4, sqlTime);

                preparedStatement.executeUpdate();
            } else {
                this.removeTicket(ticket);
                ticket = null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        //TODO: check if ticket has already been used

        //TODO: implement insertion
        return ticket;
    }

    @Lock(LockType.WRITE)
    private boolean checkForward(Connection connect, int seats, java.sql.Date sqlDate, java.sql.Time sqlTime) {
        try {
            Statement statement;
            statement = connect.createStatement();
            String query = "select sum(seats) from tablemanagerdb.reservations\n"
                    + "where date = cast('" + sqlDate.toString() + "' as date)\n"
                    + "and timediff(time, '" + sqlTime.toString() + "') < cast('03:00:00' as time)\n"
                    + "and timediff(time, '" + sqlTime.toString() + "') > cast('00:00:00' as time);";
            resultSet = statement
                    .executeQuery(query);

            int usedSeats = 0;
            while (resultSet.next()) {
                usedSeats = resultSet.getInt(1);
            }
            int usedTables = ((usedSeats % 2) == 0) ? usedSeats / 2 : (usedSeats + 1) / 2;
            int neededTables = ((seats % 2) == 0) ? seats / 2 : (seats + 1) / 2;
            if (usedTables + neededTables <= Properties.NUMBER_OF_TABLES) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    @Lock(LockType.WRITE)
    private boolean checkBackward(Connection connect, int seats, java.sql.Date sqlDate, java.sql.Time sqlTime) {
        try {
            Statement statement;
            statement = connect.createStatement();
            String query = "select sum(seats) from tablemanagerdb.reservations\n"
                    + "where date = cast('" + sqlDate.toString() + "' as date)\n"
                    + "and timediff(time, '" + sqlTime.toString() + "') < cast('00:00:00' as time)\n"
                    + "and timediff(time, '" + sqlTime.toString() + "') > cast('-03:00:00' as time);";
            resultSet = statement
                    .executeQuery(query);

            int usedSeats = 0;
            while (resultSet.next()) {
                usedSeats = resultSet.getInt(1);
            }
            int usedTables = ((usedSeats % 2) == 0) ? usedSeats / 2 : (usedSeats + 1) / 2;
            int neededTables = ((seats % 2) == 0) ? seats / 2 : (seats + 1) / 2;
            if (usedTables + neededTables <= Properties.NUMBER_OF_TABLES) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    @Lock(LockType.WRITE)
    public List<RealTimeRequest> getStoredNormalRequestsList() {
        List<RealTimeRequest> mList = new ArrayList<>();

        this.openConnection();
        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("select * from tablemanagerdb.normalrequestslist");

            while (resultSet.next()) {
                RealTimeRequest r = new RealTimeRequest(0, null, Long.MIN_VALUE);
                r.setSeats(resultSet.getInt("seats"));
                r.setSittingTime(resultSet.getTime("sittingTime"));
                r.setTicket(resultSet.getString("ticket"));
                r.setUserId(resultSet.getLong("userId"));
                mList.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mList;
    }

    @Lock(LockType.WRITE)
    public List<RealTimeRequest> getStoredServedRequestsList() {
        List<RealTimeRequest> mList = new ArrayList<>();

        this.openConnection();
        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("select * from tablemanagerdb.servedrequestslist");

            while (resultSet.next()) {
                RealTimeRequest r = new RealTimeRequest(0, null, Long.MIN_VALUE);
                r.setSeats(resultSet.getInt("seats"));
                r.setSittingTime(resultSet.getTime("sittingTime"));
                r.setTicket(resultSet.getString("ticket"));
                r.setUserId(resultSet.getLong("userId"));
                mList.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mList;
    }

    @Lock(LockType.WRITE)
    public Queue<Reservation> getStoredReservationsList() {
        Queue<Reservation> mList = new ArrayDeque<>();

        this.openConnection();
        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("select * from tablemanagerdb.reservationslist");

            while (resultSet.next()) {
                Reservation r = new Reservation();
                r.setSeats(resultSet.getInt("seats"));
                r.setSittingTime(resultSet.getTime("sittingTime"));
                r.setTicket(resultSet.getString("ticket"));
                r.setDate(resultSet.getDate("date"));
                r.setTime(resultSet.getTime("time"));
                r.setId(resultSet.getInt("id"));
                r.setSeatsTmp(resultSet.getInt("seatsTmp"));
                r.setTableId(resultSet.getInt("tableId"));
                mList.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mList;
    }

    @Lock(LockType.WRITE)
    public List<Reservation> getStoredReadyReservationsList() {
        List<Reservation> mList = new ArrayList<>();

        this.openConnection();
        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("select * from tablemanagerdb.readyreservationslist");

            while (resultSet.next()) {
                Reservation r = new Reservation();
                r.setSeats(resultSet.getInt("seats"));
                r.setSittingTime(resultSet.getTime("sittingTime"));
                r.setTicket(resultSet.getString("ticket"));
                r.setDate(resultSet.getDate("date"));
                r.setTime(resultSet.getTime("time"));
                r.setId(resultSet.getInt("id"));
                r.setSeatsTmp(resultSet.getInt("seatsTmp"));
                r.setTableId(resultSet.getInt("tableId"));
                mList.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mList;
    }

    @Lock(LockType.WRITE)
    public List<Reservation> getStoredServedReservationsList() {
        List<Reservation> mList = new ArrayList<>();

        this.openConnection();
        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("select * from tablemanagerdb.servedreservationslist");

            while (resultSet.next()) {
                Reservation r = new Reservation();
                r.setSeats(resultSet.getInt("seats"));
                r.setSittingTime(resultSet.getTime("sittingTime"));
                r.setTicket(resultSet.getString("ticket"));
                r.setDate(resultSet.getDate("date"));
                r.setTime(resultSet.getTime("time"));
                r.setId(resultSet.getInt("id"));
                r.setSeatsTmp(resultSet.getInt("seatsTmp"));
                r.setTableId(resultSet.getInt("tableId"));
                mList.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mList;
    }

    @Lock(LockType.WRITE)
    public Queue<Integer> getStoredAvailableTableIds() {
        Queue<Integer> mList = new ArrayDeque<>();

        this.openConnection();
        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("select * from tablemanagerdb.availabletableids");

            while (resultSet.next()) {
                mList.add(resultSet.getInt("id"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mList;
    }

    @Lock(LockType.WRITE)
    public Queue<Integer> getStoredUsedTableIds() {
        Queue<Integer> mList = new ArrayDeque<>();

        this.openConnection();
        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("select * from tablemanagerdb.usedtableids");

            while (resultSet.next()) {
                mList.add(resultSet.getInt("id"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mList;
    }

    @Lock(LockType.WRITE)
    public Integer getStoredNumberOfAvailableTableUnits() {
        Integer value = new Integer(0);

        this.openConnection();
        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("select * from tablemanagerdb.numberofavailabletableunits");

            while (resultSet.next()) {
                value = resultSet.getInt("num");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    @Lock(LockType.WRITE)
    public List<TableUnit> getStoredTableUnits() {
        List<TableUnit> mList = new ArrayList<>();

        this.openConnection();
        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("select * from tablemanagerdb.tableunits");

            while (resultSet.next()) {
                TableUnit r = new TableUnit();
                r.setTableId(resultSet.getInt("tableId"));
                r.setFree(resultSet.getBoolean("free"));
                r.setTicket(resultSet.getString("ticket"));
                r.setId(resultSet.getInt("id"));
                mList.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mList;
    }

    @Lock(LockType.WRITE)
    public boolean hasBackUpData() {
        this.openConnection();
        int count = 0;
        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("select count(*) from tablemanagerdb.availabletableids");
            if (resultSet.next()) {
                count += resultSet.getInt(1);
            }
            resultSet = statement.executeQuery("select count(*) from tablemanagerdb.normalrequestslist");
            if (resultSet.next()) {
                count += resultSet.getInt(1);
            }
            resultSet = statement.executeQuery("select count(*) from tablemanagerdb.numberofavailabletableunits");
            if (resultSet.next()) {
                count += resultSet.getInt(1);
            }
            resultSet = statement.executeQuery("select count(*) from tablemanagerdb.readyreservationslist");
            if (resultSet.next()) {
                count += resultSet.getInt(1);
            }
            resultSet = statement.executeQuery("select count(*) from tablemanagerdb.reservationslist");
            if (resultSet.next()) {
                count += resultSet.getInt(1);
            }
            resultSet = statement.executeQuery("select count(*) from tablemanagerdb.servedrequestslist");
            if (resultSet.next()) {
                count += resultSet.getInt(1);
            }
            resultSet = statement.executeQuery("select count(*) from tablemanagerdb.servedreservationslist");
            if (resultSet.next()) {
                count += resultSet.getInt(1);
            }
            resultSet = statement.executeQuery("select count(*) from tablemanagerdb.tableunits");
            if (resultSet.next()) {
                count += resultSet.getInt(1);
            }
            resultSet = statement.executeQuery("select count(*) from tablemanagerdb.usedtableids");
            if (resultSet.next()) {
                count += resultSet.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if(count!=0)
            return true;
        else
            return false;
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void storeNormalRequest(RealTimeRequest rtr){
        try{
            this.openConnection();
            preparedStatement = connect.prepareStatement("insert into tablemanagerdb.normalrequestslist"
                    + " values (null, ?, ?, ?, ?)");
            preparedStatement.setInt(1, rtr.getSeats());
            preparedStatement.setLong(2, rtr.getUserId());
            preparedStatement.setString(3, rtr.getTicket());
            if(rtr.getSittingTime()!=null)
                preparedStatement.setTime(4, new java.sql.Time(rtr.getSittingTime().getTime()));
            else
                preparedStatement.setDate(4, null);
            preparedStatement.executeUpdate();
        }catch(Exception e){ e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void removeNormalRequest(RealTimeRequest rtr){
        try{
            openConnection();
            statement = connect.createStatement();
            statement.execute("DELETE FROM tablemanagerdb.normalrequestslist WHERE userId=\"" + rtr.getUserId() + "\";");
        }catch(Exception e){e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void storeServedRequest(RealTimeRequest rtr){
        try{
            this.openConnection();
            preparedStatement = connect.prepareStatement("insert into tablemanagerdb.servedrequestslist"
                    + " values (?, ?, ?, ?)");
            preparedStatement.setInt(1, rtr.getSeats());
            preparedStatement.setLong(2, rtr.getUserId());
            preparedStatement.setString(3, rtr.getTicket());
            if(rtr.getSittingTime()!=null)
                preparedStatement.setTime(4, new java.sql.Time(rtr.getSittingTime().getTime()));
            else
                preparedStatement.setDate(4, null);
            preparedStatement.executeUpdate();
        }catch(Exception e){ e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void removeServedRequest(RealTimeRequest rtr){
        try{
            openConnection();
            statement = connect.createStatement();
            statement.execute("DELETE FROM tablemanagerdb.servedrequestslist WHERE ticket=\"" + rtr.getTicket()+ "\";");
        }catch(Exception e){e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void storeReservation(Reservation re){
        try{
            this.openConnection();
            preparedStatement = connect.prepareStatement("insert into tablemanagerdb.reservationslist"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, re.getId());
            preparedStatement.setString(2, re.getTicket());
            preparedStatement.setInt(3, re.getSeats());
            preparedStatement.setInt(4, re.getSeatsTmp());
            preparedStatement.setInt(4, re.getSeatsTmp());
            if(re.getDate()!=null)
                preparedStatement.setDate(5, new java.sql.Date(re.getDate().getTime()));
            else
                preparedStatement.setDate(5, null);
            if(re.getTime()!=null)
                preparedStatement.setTime(6, new java.sql.Time(re.getTime().getTime()));
            else
                preparedStatement.setTime(6, null);
            preparedStatement.setInt(7, re.getTableId());
            if(re.getSittingTime()!=null)
                preparedStatement.setTime(8, new java.sql.Time(re.getSittingTime().getTime()));
            else
                preparedStatement.setTime(8, null);
            preparedStatement.executeUpdate();
        }catch(Exception e){ e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void removeReservation(Reservation re){
        try{
            openConnection();
            statement = connect.createStatement();
            statement.execute("DELETE FROM tablemanagerdb.reservationslist WHERE id=\"" + re.getId() + "\";");
        }catch(Exception e){e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void storeReadyReservation(Reservation re){
        try{
            this.openConnection();
            preparedStatement = connect.prepareStatement("insert into tablemanagerdb.readyreservationslist"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, re.getId());
            preparedStatement.setString(2, re.getTicket());
            preparedStatement.setInt(3, re.getSeats());
            preparedStatement.setInt(4, re.getSeatsTmp());
            preparedStatement.setInt(4, re.getSeatsTmp());
            if(re.getDate()!=null)
                preparedStatement.setDate(5, new java.sql.Date(re.getDate().getTime()));
            else
                preparedStatement.setDate(5, null);
            if(re.getTime()!=null)
                preparedStatement.setTime(6, new java.sql.Time(re.getTime().getTime()));
            else
                preparedStatement.setTime(6, null);
            preparedStatement.setInt(7, re.getTableId());
            if(re.getSittingTime()!=null)
                preparedStatement.setTime(8, new java.sql.Time(re.getSittingTime().getTime()));
            else
                preparedStatement.setTime(8, null);
            preparedStatement.executeUpdate();
        }catch(Exception e){ e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void removeReadyReservation(Reservation re){
        try{
            openConnection();
            statement = connect.createStatement();
            statement.execute("DELETE FROM tablemanagerdb.readyreservationslist WHERE id=\"" + re.getId() + "\";");
        }catch(Exception e){e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void storeServedReservation(Reservation re){
        try{
            this.openConnection();
            preparedStatement = connect.prepareStatement("insert into tablemanagerdb.servedreservationslist"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, re.getId());
            preparedStatement.setString(2, re.getTicket());
            preparedStatement.setInt(3, re.getSeats());
            preparedStatement.setInt(4, re.getSeatsTmp());
            preparedStatement.setInt(4, re.getSeatsTmp());
            if(re.getDate()!=null)
                preparedStatement.setDate(5, new java.sql.Date(re.getDate().getTime()));
            else
                preparedStatement.setDate(5, null);
            if(re.getTime()!=null)
                preparedStatement.setTime(6, new java.sql.Time(re.getTime().getTime()));
            else
                preparedStatement.setTime(6, null);
            preparedStatement.setInt(7, re.getTableId());
            if(re.getSittingTime()!=null)
                preparedStatement.setTime(8, new java.sql.Time(re.getSittingTime().getTime()));
            else
                preparedStatement.setTime(8, null);
            preparedStatement.executeUpdate();
        }catch(Exception e){ e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void removeServedReservation(Reservation re){
        try{
            openConnection();
            statement = connect.createStatement();
            statement.execute("DELETE FROM tablemanagerdb.servedreservationslist WHERE id=\"" + re.getId() + "\";");
        }catch(Exception e){e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void storeNumberOfAvailableTableUnits(int num){
        try{
            openConnection();
            preparedStatement = connect.prepareStatement("INSERT into tablemanagerdb.numberofavailabletableunits"
                    + " values (?, ?) ON DUPLICATE KEY UPDATE num=\" "+ num +"\"");
            preparedStatement.setInt(1, 1);
            preparedStatement.setInt(2, num);
            preparedStatement.executeUpdate();
        }catch(Exception e){e.printStackTrace();}
        
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void storeTableUnit(TableUnit t){
        try{
            openConnection();
            
            preparedStatement = connect.prepareStatement("INSERT into tablemanagerdb.tableunits"
                    + " values (?, ?, ?, ?) ON DUPLICATE KEY UPDATE tableId=\" "+ t.getTableId() +"\", free=\""+ ((t.isFree())?1:0) +"\", ticket=\""+ t.getTicket() +"\" ");
            preparedStatement.setInt(1, t.getId());
            preparedStatement.setInt(2, t.getTableId());
            preparedStatement.setInt(3, ((t.isFree())?1:0));
            preparedStatement.setString(4, t.getTicket());
            preparedStatement.executeUpdate();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void storeAvailableTableId(int id){
        try{
            openConnection();
            preparedStatement = connect.prepareStatement("insert into tablemanagerdb.availabletableids"
                    + " values (?);");
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        }catch(Exception e){e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void removeAvailableTableId(int id){
        try{
            openConnection();
            statement = connect.createStatement();
            statement.execute("DELETE FROM tablemanagerdb.availabletableids WHERE id=\""+ id +"\";");
        }catch(Exception e){e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void storeUsedTableId(int id){
        try{
            openConnection();
            preparedStatement = connect.prepareStatement("insert into tablemanagerdb.usedtableids"
                    + " values (?);");
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        }catch(Exception e){e.printStackTrace();}
    }
    
    @Lock(LockType.WRITE)
    @Asynchronous
    public void removeUsedTableId(int id){
        try{
            openConnection();
            statement = connect.createStatement();
            statement.execute("DELETE FROM tablemanagerdb.usedtableids WHERE id=\""+ id +"\";");
        }catch(Exception e){e.printStackTrace();}
    }
}
