import javafx.collections.ObservableList;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Created by barthclem on 8/2/16.
 */
public interface Candidates extends Remote{
    public void calculateScore() throws RemoteException;
    public void displayQuestion(QuixEntry entry) throws RemoteException;
    public void disableOption() throws RemoteException;
    public void disableButton() throws RemoteException;
    public void enableButton() throws RemoteException;
    public void enableBonus(boolean enhance) throws RemoteException;
    public void enableOption() throws RemoteException;
    public String getEligibityStatus() throws RemoteException;
    public String getName() throws RemoteException;
    public String getGroup() throws RemoteException;
    public int getScore() throws RemoteException;
    public void join() throws RemoteException;
    public void notifyQuestion(QuixEntry entry) throws RemoteException;
    public void notifyBonus() throws RemoteException;
    public void pickQuestion() throws RemoteException;
    public void setAvailableQuestions(ArrayList<Integer> list)throws RemoteException;
    public void setSession(String session) throws RemoteException;
    public void startBonusTimer() throws RemoteException;
    public  void  setEligibityStatus(String el) throws RemoteException;
    public void stopTimer() throws RemoteException;
    public long totalTime() throws RemoteException;//this returns the total time spent answering all the questions
    public void setInfo(String info) throws RemoteException;
    public void startQuestionTimer() throws RemoteException;
    public void startRecessTimer() throws RemoteException;
    public void resultPage(ObservableList<CandidateEntry> list,int position) throws RemoteException;

}
