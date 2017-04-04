import java.io.Serializable;

/**
 * Created by barthclem on 8/4/16.
 */
public class QuixEntry implements Serializable{
    private String question,option1,option2,option3,option4,answer;
    private int index;

    public QuixEntry(String question, String option1, String option2, String option3, String option4, String answer, int index) {
        this.question = question;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.answer = answer;
        this.index = index;
    }

    public String getQuestion() {
        return question;
    }

    public String getOption1() {
        return option1;
    }

    public String getOption2() {
        return option2;
    }

    public String getOption3() {
        return option3;
    }

    public String getOption4() {
        return option4;
    }

    public String getAnswer() {
        return answer;
    }

    public int getIndex() {
        return index;
    }
}
