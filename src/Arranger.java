import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by barthclem on 10/3/16.
 */
public class Arranger {
    public ArrayList<Candidates> arrg=new ArrayList<>();

    public List<Candidates> getList(){
        return arrg;
    }

    public void add(Candidates candidate){
        arrg.add(candidate);
        int currPos=arrg.size()-1;
        try{
        while(currPos>0&&arrg.get(currPos-1).totalTime()>candidate.totalTime()){
            arrg.set(currPos,arrg.get(currPos-1));
            currPos--;
        }
        arrg.set(currPos,candidate);
        }
        catch(RemoteException es){
            es.printStackTrace();
        }
    }
}
