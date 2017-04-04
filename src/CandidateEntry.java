import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Created by barthclem on 8/4/16.
 */
public class CandidateEntry {
    private  StringProperty name;
    private  StringProperty group;
    private  StringProperty status;
    private  StringProperty score;

    public CandidateEntry(String name,String group,String status,int score){

        this.name=new SimpleStringProperty(name);
        this.group=new SimpleStringProperty(group);
        this.status=new SimpleStringProperty(status);
        this.score=new SimpleStringProperty(Integer.toString(score));

    }


    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getGroup() {
        return group.get();
    }

    public StringProperty groupProperty() {
        return group;
    }

    public void setGroup(String group) {
        this.group.set(group);
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public int getScore() {
        return Integer.parseInt(score.get());
    }

    public StringProperty scoreProperty() {
        return score;
    }

    public void setScore(String score) {
        this.score.set(score);
    }
}