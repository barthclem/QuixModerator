import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by barthclem on 8/4/16.
 */
public interface Moderator extends Remote{
    public void RegisterCandidate(Candidates candidate) throws RemoteException;
    public void pickQuestion(int questionno) throws RemoteException;
    public void  answerQuestion(String answer) throws RemoteException;
   }
