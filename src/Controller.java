import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;


public class Controller {
    @FXML
    private Label IPAddressLabel;
    @FXML
    private Label InformationLabel;
    @FXML
    private Label TopicLabel;
    @FXML
    private  Label SessionLabel;
    @FXML
    private Label TimeLabel;
    @FXML
    private TableView<CandidateEntry> tableView;
    @FXML
    private TableColumn<CandidateEntry,String> NameColumn;
    @FXML
    private TableColumn<CandidateEntry,String> GroupColumn;
    @FXML
    private TableColumn<CandidateEntry,String> StatusColumn;
    @FXML
    private TableColumn<CandidateEntry,String> ScoreColumn;

    private ObservableList<CandidateEntry> candidateEntries= FXCollections.observableArrayList();

    private ArrayList<Candidates> candidateslist=new ArrayList<>();
    private ArrayList<Candidates> allcandidateslist=new ArrayList<>();
    private ArrayList<Candidates> contestantsList=new ArrayList<>();//this is the list of candidates that have to contest a position
    private ArrayList<QuixEntry> unAnsweredQuestions=new ArrayList<>();
    private ArrayList<String> availableQuestions;
    private ArrayList<Integer> providableQuestions=new ArrayList<>();
    private Deque<String> positions=new ArrayDeque<>();
    private String ipAddress;//ip address of the rmi registry where the stub object is hosted
    private String table,answer;//the database table that contains the questions
    private Map<Integer,String> sessionsMap=new HashMap<>();
    private Map<Integer,Arranger> scores=new TreeMap<>();
    private Map<Integer,Integer> candidateScores=new HashMap<>();
    private Map<String,ArrayList<QuixEntry>> sessionQuestion=new HashMap<>();
    private Connection conn;
    private int questionNumber,numberOfSessions;
    private boolean bonusEnabled,hasNotPickedQuestion=true,questionType;
    private static volatile boolean pickQuestionCondition=true,answerQuestionCondition=true,answerBonusQuestion=true;
    private Thread pickQuestionT,answerQuestionT,answerBonusT;
    private Lock lock;
    private Condition waitCondition,questionDelay,bonusDelay,waitCondition1,questionDelay1,bonusDelay1;



    @FXML
    private void initialize(){

        NameColumn.setCellValueFactory(cellData->(cellData.getValue().nameProperty()));
        GroupColumn.setCellValueFactory(cellData->(cellData.getValue().groupProperty()));
        StatusColumn.setCellValueFactory(cellData->(cellData.getValue().statusProperty()));
        ScoreColumn.setCellValueFactory(cellData->(cellData.getValue().scoreProperty()));
        lock=new ReentrantLock();
        waitCondition=lock.newCondition();
        questionDelay=lock.newCondition();
        bonusDelay=lock.newCondition();
        waitCondition1=lock.newCondition();
        questionDelay1=lock.newCondition();
        bonusDelay1=lock.newCondition();
    }

    @FXML
    private void startQuix(){
        new Thread(()->{
            try {
                enabledBonus();
                Platform.runLater(()->{setInformationLabel("Quix Started");});
                giveCandidateRecess();
                Thread.sleep(20000);
                moderate();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void initializePositions(){
        String[] lists={"First","Second","Third","Fourth","Fifth","Sixth","Seventh","Eighth","Ninth",
                        "Tenth","Eleventh","Twelfth","Thirteenth","Fourteenth","Fifteenth","Sixteenth",
                        "Seventeenth","Eighteenth","Nineteenth","Twentieth"};
        for(int i=0;i<lists.length;i++){
            positions.add(lists[i]);
        }
    }
    private void setIPAddressLabel(String ip){
        Platform.runLater(()->{IPAddressLabel.setText("MODERATOR :"+ip);});
    }


    private void setInformationLabel(String information) {
        Platform.runLater(()->{InformationLabel.setText("INFO : "+information);});
    }

    private void setTopicLabel(String topic) {
        Platform.runLater(()->{
            TopicLabel.setText("TOPIC : "+topic);
        });
    }

    private void setSessionLabel(String session) {
        Platform.runLater(()->{SessionLabel.setText("SESSION : "+session);});
    }

    private void setTimeLabel(String time) {
        Platform.runLater(()->{TimeLabel.setText("TIME : "+time);});
    }

    private void setTableView(ObservableList<CandidateEntry> candidateEntries){
        Platform.runLater(()->{tableView.setItems(candidateEntries);});
    }

    private void setSessionsMap(String table){
        try {
            conn=Connections.getConnection();
            Statement statement=conn.createStatement();
            String sql="select * from "+table+"Sessions";
            ResultSet resultSet=statement.executeQuery(sql);
            while(resultSet.next()){
                String title=resultSet.getString("name");
                int id=resultSet.getInt("id");
                sessionsMap.put(id,title);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void setSessionQuestion(String table){
        try {

            Set<Map.Entry<Integer, String>> set = sessionsMap.entrySet();
            Iterator<Map.Entry<Integer, String>> iterator = set.iterator();
            while (iterator.hasNext()) {
                ArrayList<QuixEntry> quixEntries=new ArrayList<>();
                Map.Entry<Integer, String> entry = iterator.next();
                int id = entry.getKey();
                String session=entry.getValue();
                String sql = "select * from " + table + " where section = ?" ;
                PreparedStatement stat = Connections.getConnection().prepareStatement(sql);
                stat.setInt(1,id);
                ResultSet resultSet=stat.executeQuery();
                int index=1;
                while(resultSet.next()){
                    quixEntries.add(new QuixEntry(
                            resultSet.getString("questions"),
                            resultSet.getString("option1"),
                            resultSet.getString("option2"),
                            resultSet.getString("option3"),
                            resultSet.getString("option4"),
                            resultSet.getString("answer"),
                            index++));
                }

                sessionQuestion.put(session,quixEntries);
            }
        }
        catch (SQLException e){e.printStackTrace();} catch (IOException e) {
            e.printStackTrace();
        }

    }


    private int getEligibleCandidates(){
        int no=0;
        for(Candidates candidate:candidateslist){
            try {
                if(candidate.getEligibityStatus().equals("qualified")){

                    no++;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return  no;

    }

    private void initAvailableQuestion(ArrayList<QuixEntry> quixEntries){
        availableQuestions=new ArrayList<>();
        for(QuixEntry entry:quixEntries){
            availableQuestions.add(""+entry.getIndex());
            providableQuestions.add(entry.getIndex());
        }
        setCandidatesAvailableQuestion();
    }

    private void updateAvailableQuestion(int index){

        availableQuestions.remove(""+index);
        providableQuestions=new ArrayList<>();
        for(String i:availableQuestions){
            providableQuestions.add(Integer.parseInt(i));
        }
        setCandidatesAvailableQuestion();
    }

    private void setCandidatesAvailableQuestion(){
        for(Candidates candidates:candidateslist){
            try {
                candidates.setAvailableQuestions(providableQuestions);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void tellOthersIHaveQuestion(){
        for(Candidates candidates:candidateslist){
            try {
                candidates.stopTimer();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void setCandidateSession(String session){
        for(Candidates candidate:candidateslist){
            try {
                candidate.setSession(session);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void candidatePick(int can){
        Candidates candidate=candidateslist.get(can);
        for(Candidates candidates:candidateslist){
            try {
                if (candidates == candidate) {
                    candidates.stopTimer();
                     Platform.runLater(()->{
                        try {
                            InformationLabel.setText("Candidate to pick question is : "+candidates.getName());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                    });
                    candidates.pickQuestion();

                }
                else{
                    candidates.stopTimer();
                    candidates.disableButton();//disable all other candidates from picking question
                }
            }
            catch (IOException e){

            }
        }

    }

    private Candidates searchCandidate(String name,String group){
        //assumming that names and groups  are unique
        Candidates foundCandidate=null;
        for(Candidates candidates:candidateslist){
            try {
                if(candidates.getName().equals(name)&&candidates.getGroup().equals(group)){
                    foundCandidate=candidates;
                    break;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return foundCandidate;
    }

    private void distributeQuestion(int can,QuixEntry entry){
        Candidates candidate=candidateslist.get(can);
        for(Candidates candidates:candidateslist){
            try {
                if (candidates == candidate) {
                    candidates.stopTimer();
                    candidates.notifyQuestion(entry);

//                    candidates.startQuestionTimer();
                }
                else{
                    candidates.disableButton();//disable all other candidates from
                    candidates.displayQuestion(entry);//disallow other candidate from answering questions
                    candidates.startQuestionTimer();
                }

            }
            catch (IOException e){

            }

        }

    }

    private void giveBonus(int can){
        int nextCandidate=can+1;
        if(nextCandidate==candidateslist.size())
        {
            nextCandidate=0;}
        Candidates candidate=candidateslist.get(nextCandidate);
        for(Candidates candidates:candidateslist){
            try {
                if (candidates == candidate) {
                       candidates.notifyBonus();
                }
                else{
                    candidates.disableButton();//disable all other candidates from
                    candidates.startBonusTimer();
                }
            }
            catch (IOException e){

            }
        }

    }

    //Not ready to complicate a simple app with some multi thread nonsense ...
    //for the delay i employ the use of while loop concepts you know


    private void pickWaitCondition(){
  //      new Thread(()->{

            try {
//                lock.lock();
//                System.out.println("\nThread started");
//                waitCondition.await();
//                System.out.println("\n\nSignal Received for picking\n\n");
                int i=0;
                while(i<15&&pickQuestionCondition)
                {
                    Thread.sleep(1000);
                    i++;
                    }

//                waitCondition1.signal();
                pickQuestionCondition=true;
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
//            finally{
//                lock.unlock();
//            }


//        }).start();

    }
    private void questionWaitCondition(){
    //    new Thread(()->{

            try {
//                lock.lock();
//                System.out.println("Thread started");
//                questionDelay.await();
//                System.out.println("\n\nSignal Received for question\n\n");
                int i=0;
                while(i<45&&answerQuestionCondition)
                {Thread.sleep(1000);
                    i++;
                    }

//                questionDelay1.signal();
                answerQuestionCondition=true;

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            finally{
//                lock.unlock();
//            }

      //  }).start();


    }
    private void bonusWaitCondition(){
        //new Thread(()->{

            try {
//                lock.lock();
//                System.out.println("Thread started");
//                bonusDelay.await();
//                System.out.print("\n\nSignal Received for bonus\n\n");
                int i=0;
                while(i<15&&answerBonusQuestion) {
                    Thread.sleep(1000);
                    i++;
                }
//                bonusDelay1.signal();
                answerBonusQuestion=true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            finally {
//                lock.unlock();
//            }

        //}).start();


    }
    private void giveCandidateRecess() {
        for (Candidates candidates : candidateslist) {
            try {
                candidates.startRecessTimer();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void enabledBonus() {
        for (Candidates candidates : candidateslist) {
            try {
                candidates.enableBonus(bonusEnabled);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void setNumberOfSession(){
        numberOfSessions=sessionQuestion.size();
    }

    private ArrayList<Scores> getAllScores() {
        ArrayList<Scores> scoresHolder = new ArrayList<>();
        for (CandidateEntry candidates : candidateEntries) {
            if (candidates.getStatus().equals("qualified")) {
                int score = candidates.getScore();
                boolean occurred = false;
                for (int i = 0; i < scoresHolder.size(); i++) {
                    //search through the scores and occurrence map to see whether the score already exits
                    if (score == scoresHolder.get(i).getScores()) {
                        scoresHolder.get(i).increasePopulation();
                        occurred = true; //the boolean is set to true if the score already exist
                        System.out.print("Score " + score + " no of Occurrence :" + scoresHolder.get(i).getPopulation());
                    }
                }
                if (!occurred) {
                    //if there is zero occurence it can only mean that the score is new hence , we need to insert into the list
                    //we need to sort the list with the new input
                    Scores scores = new Scores(score, 1);
                    scoresHolder.add(scores);
                    int index = scoresHolder.indexOf(scores);
                    System.out.println("The  index of the score is : " + index);
                    if (index >= 1) {//this is strictly experimental
                        while (index > 0 && scores.getScores() > scoresHolder.get(index - 1).getScores()) {
                            scoresHolder.set(index--, scoresHolder.get(index - 1));
                        }
                        scoresHolder.set(index, scores);
                    }
                }

            }
        }
            return scoresHolder;


    }

    private int getGoodNoOfCandidates(int currentIndex){
        //this is used to get a good number of candidates such that there are at least two rounds
        //this is used to select qualified candidates based on a definition that depends on current index and total number of sessions
        int noOfEligible=getEligibleCandidates();
        System.out.println("Previous number of eligible candidates is : "+noOfEligible);
        System.out.println("The current index of the session is  : "+currentIndex);
        int goodNumber=numberOfSessions-currentIndex;
         goodNumber=(goodNumber==0)?1:goodNumber;
        System.out.println("A good number of candidates that can qualify is : "+goodNumber);
        int candidateDisqualified=(int)(((double)goodNumber/(double)numberOfSessions)*(noOfEligible));
        System.out.println("The number of those disqualified is : "+candidateDisqualified);
        int currentEligible=noOfEligible-candidateDisqualified;
        System.out.println("A good number of candidates that can qualify is : "+currentEligible);
        return  currentEligible;
    }

    private ArrayList<Scores> markRequired(ArrayList<Scores> scores,int eligibleNumber){
        int i=0;
        //pending logic to improve this part
        System.out.println("\n\nThe current size of scores list is : "+scores.size());
        while(eligibleNumber>0&&i<scores.size()){
            scores.get(i).setMarked(true);
            eligibleNumber=eligibleNumber-scores.get(i).getPopulation();//this forces the loop to stop once the  required number of candidates is complete
            System.out.println("Eligible score : "+scores.get(i).getScores());

            i++;
             }

        return scores;
    }

    private void disQualifyCandidates(){
        for(int j=0;j<candidateslist.size();j++){
            try {
                //disqualify all at the beginning ...****
                if(!candidateslist.get(j).getEligibityStatus().equals("qualified")) {
                    candidateslist.remove(candidateslist.get(j));
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    private void setEligibityStatus(ArrayList<Scores> scoreslist,int qualifiedNumber){
        int i=0;
        for(int j=0;j<candidateslist.size();j++){
            try {
                //disqualify all at the beginning ...****
                candidateslist.get(j).setEligibityStatus("not qualified");

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        while(i<scoreslist.size()){ //the scores list already has the list of acceptable scores
            System.out.println(scoreslist.get(i).getScores()+"  is marked ? : "+scoreslist.get(i).isMarked());
            if(scoreslist.get(i).isMarked()&& scoreslist.get(i).getPopulation()<=qualifiedNumber){ //we checked if the score  was marked as acceptable and the number of
                                          //remaining candidates that can be qualified is less than the number of
                                            //candidates having that score.
                for(int j=0;j<candidateslist.size();j++){//go through the candidate list to select candidates with the same score
                    if(scoreslist.get(i).getPopulation()>0) {
                        try {
                            if (candidateslist.get(j).getScore() == scoreslist.get(i).getScores()) {
                                //if a candidate has score in the range of the acceptable scores then set its eligibity to acceptable
                                scoreslist.get(i).decreasePopulation();// here decrease the number of scores in that range by one
                                qualifiedNumber--;//this keeps count of the number of candidates that has been qualified
                                //we mark them out here
                                System.out.println(candidateslist.get(j).getName() +"  is qualified \n\n\n");
                                candidateslist.get(j).setEligibityStatus("qualified");
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        break;
                    }
                }

            }
            else if(scoreslist.get(i).isMarked()&& scoreslist.get(i).getPopulation()>qualifiedNumber){
                //all candidates with that score are passed into an arraylist and sorted
//                ArrayList<Candidates> scoresList=new ArrayList<>();
//                for(int j=0;j<candidateslist.size();j++){
//                    try {
//                        if (candidateslist.get(j).getScore() == scoreslist.get(i).getScores()) {
//                            scoresList.add(candidateslist.get(j));
//                        }
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//                }
//                //The list is then sorted
//                Deque<Candidates> candidatesQueue=sortQualiifiedCandidates(scoresList);
//                while(qualifiedNumber>=0){
//                    try{
//                        //this qualifies the first n number of candidates in the queue
//                        //where n is the remaining number of candidates needed to be qualified
//                        Candidates candidates=candidatesQueue.pop();
//                        candidates.setEligibityStatus("qualified");
//                        qualifiedNumber--;//PAUSED**********8
//                    }
//                    catch(RemoteException e){
//                        e.printStackTrace();
//                    }
                try{
                    int score=scoreslist.get(i).getScores();
                    populateScores();
                    Arranger arranger=scores.get(score);
                    List<Candidates> list=arranger.getList();//fetch the list of candidates with this score
                    int counter=0;
                    while(qualifiedNumber>0){
                        list.get(counter++).setEligibityStatus("qualified");
                    }
                    //clear the scores Map
                    scores.clear();

                }
                catch(RemoteException remote){}


            }
            else{

            }
            i++; //increment the counter
        }
        disQualifyCandidates();

    }

    private void moderateKnockOut(String knockType){
        //this is the method that hanles the settling of issues with candidates having the same score after all game
        int noOfContestants=contestantsList.size();
        String position=knockType;
        int contestContinues=0;
        setSessionLabel("Knock Out");
        setCandidateSession("Knock Out");
        ArrayList<QuixEntry> questions=(ArrayList<QuixEntry>) unAnsweredQuestions.clone();
        unAnsweredQuestions.clear();
        while(noOfContestants!=1&&contestContinues>=0){
            setCandidatesAvailableQuestion();
            int round=questions.size()-(questions.size()%noOfContestants);
            System.out.println("Number of Rounds : "+round);
            for(int i=0;i<round;i++){
                int candidate=(i%noOfContestants);//this make the selection of the candidate circular
                System.out.println("Candidate Index : "+candidate);
                candidatePick(candidate);//this method allows a candidate to pick a question
                pickWaitCondition();
                questionType=true;
                if(pickedQuestion()){
                    //I am sorry this result in an endless loop until the candidate pick a question
                    QuixEntry candidateChoice=searchForQuestion(questionNumber,questions);//this searches for the question with that index
                    distributeQuestion(candidate,candidateChoice);//wait for the candidate to pick answer
                    questionWaitCondition();
                    clearAnswer();
                    updateAvailableQuestion(questionNumber);
                    updateViewList(candidateslist.get(candidate));
                    anotherCandidatePick();//this wait for another candidate y=to pick question
                }

            }
            gatherAllUnAnswered(questions);
            //after each round examine the whole system,there might be reduction in the number of candidates
            contestContinues=knockOut(position,noOfContestants);
            noOfContestants=contestantsList.size();//hence,there is  a good reason to review the number of eligible candidates
            if(contestContinues==1&&position.equals("first")){
                position="second";
            }
            if(contestContinues==1&&position.equals("second")){
                position="third";
            }

    }

    }
    private int knockOut(String type,int no){
        int  contestContinues=0;
        //this system makes use of three integer points system
        //-1 means the contest should  stop
        //0 means no significant changes
        //1 means there is a significant  change
        if(no<=3) {
            //if the number of contestants at this stage is less than or equal to three
            Candidates highest = getKnockoutHighest();
            Candidates lowest = getKnockoutLowest();
            int highfrequency = 0;
            try {
                for (Candidates candidates : contestantsList) {
                    if (candidates.getScore() == highest.getScore()) {
                        highfrequency++;
                    }
                }
                if (highest.getScore() == lowest.getScore()) {
                    //this implies that all contestants scored the same score
                }
                if (highfrequency > 1) {
                    //this implies the frequency of the lowest score is one
                    //declare the result of the lowest and remove from list
                    contestantsList.remove(lowest);
                    int index = candidateslist.indexOf(lowest);
                    if (type.equals("first") && no == 3) {
                        sendPositionToCandidate(index, "Third");

                    } else if (type.equals("second") && no == 2) {
                        sendPositionToCandidate(index, "Third");
                        contestContinues=-1;
                    }

                } else if (highfrequency == 1) {
                    //this implies there is a winner
                    //declare his result and remove
                    contestantsList.remove(highest);
                    int index = candidateslist.indexOf(highest);
                    if (type.equals("first")) {
                        sendPositionToCandidate(index, "First");
                        contestContinues=1;
                        if (type.equals("first") && no == 2) {
                            index = candidateslist.indexOf(lowest);
                            sendPositionToCandidate(index, "Second");
                            contestContinues=-1;
                        }
                    } else if (type.equals("second")) {
                        sendPositionToCandidate(index, "Second");
                        contestContinues=1;
                        if (type.equals("second") && no == 2) {
                            index = candidateslist.indexOf(lowest);
                            sendPositionToCandidate(index, "Third");
                            contestContinues=-1;
                        }
                    } else if (type.equals("third"))
                    {
                        sendPositionToCandidate(index, "Third");
                        contestContinues=-1;
                    }


                }
            } catch (IOException e) {
            }
        }
    return contestContinues;
    }
    private Candidates getKnockoutHighest(){
        Candidates highest= null;
        try {
            highest = contestantsList.get(0).getScore()>contestantsList.get(1).getScore()?contestantsList.get(0):contestantsList.get(1);
            int i=2;
            while(i<contestantsList.size()){
                highest=highest.getScore()>contestantsList.get(i).getScore()?highest:contestantsList.get(i);
                i++;
            }
            System.out.print("\n\n  the highest guy in the knock out is : "+highest.getName());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return highest;
    }
    private Candidates getKnockoutLowest(){
        Candidates lowest= null;
        try {
            lowest = contestantsList.get(0).getScore()<contestantsList.get(1).getScore()?contestantsList.get(0):contestantsList.get(1);
            int i=2;
            while(i<contestantsList.size()){
                lowest=lowest.getScore()<contestantsList.get(i).getScore()?lowest:contestantsList.get(i);
                i++;

            }

            System.out.println("\n\n\n The knock out lowest is : "+lowest.getName());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return lowest;
    }

    private void moderate() throws InterruptedException {
        Set<Map.Entry<String, ArrayList<QuixEntry>>> set = sessionQuestion.entrySet();
        Iterator<Map.Entry<String, ArrayList<QuixEntry>>> iterator = set.iterator();
        setNumberOfSession();
        initializePositions();

        

        int checkpoint= Math.toIntExact(Math.round((double) numberOfSessions / 2));//this is the checkpoint for starting setting eligibility status
        int counter=0;
        while(iterator.hasNext()){
            Map.Entry<String, ArrayList<QuixEntry>> entry=iterator.next();
            String session=entry.getKey();
            setSessionLabel(session);
            setCandidateSession(session);
            ArrayList<QuixEntry> quixEntries=entry.getValue();//this takes questions,options and answers
            initAvailableQuestion(quixEntries);
            System.out.println("The current index of the session is  : "+numberOfSessions);// the number of session is ...


            counter++;
            if(counter>checkpoint){
                //we do eligibility check here
                int number=getGoodNoOfCandidates(counter);//number of those that should qualify for the next round
                ArrayList<Scores> scores=getAllScores();
                ArrayList<Scores> scorez=markRequired(scores,number);//this chooses those it decided to be qualified for the next round
                setEligibityStatus(scorez,number);//this sets the those that are qualified based on the criterion above
            }
            int no=getEligibleCandidates();
            System.out.println("Number of Eligible Candidates : "+no);//this gets the number of eligible candidates
            if(no>1){
                int round=quixEntries.size()-(quixEntries.size()%no);
                System.out.println("Number of Rounds : "+round);
                for(int i=0;i<round;i++){
                    int candidate=(i%no);//this make the selection of the candidate circular
                    System.out.println("Candidate Index : "+candidate);
                    candidatePick(candidate);//this method allows a candidate to pick a question
                                            //Each candidate has 15seconds to pick a question
                    //delay till the candidate pick a question
                   //this delay is introduced to enable candidate to pick question
                    pickWaitCondition();
                    //pickingQuestionDelay();
                    questionType=true;//this is use to set  button to send signal

                    if(pickedQuestion()){
                        //I am sorry this result in an endless loop until the candidate pick a question
                        QuixEntry candidateChoice=searchForQuestion(questionNumber,quixEntries);//this searches for the question with that index
                        distributeQuestion(candidate,candidateChoice);//wait for the candidate to pick answer
                        questionWaitCondition();
                      //  answeringQuestionDelay();


                        if(answer==null||!answer.equals(candidateChoice.getAnswer())){
                            questionType=false;
                            //this is the condition for bonus mark
                            if(bonusEnabled){
                                giveBonus(candidate);//wait for the candidate to answer question
                                bonusWaitCondition();
                        //        bonusQuestionDelay();

                                clearAnswer();
                                updateViewList(candidateslist.get(candidate+1==candidateslist.size()?0:candidate+1));
                            }
                        }
                        clearAnswer();
                        updateAvailableQuestion(questionNumber);
                        updateViewList(candidateslist.get(candidate));
                        anotherCandidatePick();//this wait for another candidate y=to pick question
                    }

                }


            }
            //gather all unansweredquestions here
            gatherAllUnAnswered(quixEntries);
        }
        sendResults();
        //declareWinners();
    }



    private void populateScores(){
        for(Candidates candidate:candidateslist) {
            try {
                Arranger arrange = scores.get(candidate.getScore());//get a candidates with common scores list from the list
                if (arrange != null) {
                    arrange.add(candidate);//add candidate to the already existing list
                } else {
                    arrange = new Arranger();
                    arrange.add(candidate);
                    scores.put(candidate.getScore(), arrange);
                }
            }
            catch(RemoteException es){
                es.printStackTrace();
            }
        }
    }

    private void sendResults(){
        populateScores();
        Set<Map.Entry<Integer,Arranger>> set=scores.entrySet();
        Iterator<Map.Entry<Integer,Arranger>> iterator=set.iterator();
        try {
            while (iterator.hasNext()) {
                Arranger arrange = iterator.next().getValue();
                List<Candidates> list = arrange.getList();
                for (int i = 0; i < list.size(); i++) {
                    String position = positions.pop();
                    Candidates candidate = list.get(i);
                    System.out.println("\nCandidates " + i + "    name  -->" + candidate.getName() + "" +
                                    "    score -->" + candidate.getScore() +
                                    "    time spent -->" + candidate.totalTime() +
                                    "    POSITION -->" + position+
                            "\n");
                    candidate.setInfo("The " + position + " Position goes to you");
                }
            }
        }
        catch(RemoteException remo){

            remo.printStackTrace();
        }
    }


    private void gatherAllUnAnswered(ArrayList<QuixEntry> entries){
        int i=1;
        providableQuestions.clear();
        for(QuixEntry entry:entries){
            for(String available:availableQuestions){
                if(entry.getIndex()==Integer.parseInt(available)){
                    unAnsweredQuestions.add(new QuixEntry(entry.getQuestion(),entry.getOption1(),
                            entry.getOption2(),entry.getOption3()
                    ,entry.getOption4(),entry.getAnswer(),i++));
                    providableQuestions.add(i);
                }
            }
        }
    }

    private void sendPositionToCandidate(int index,String position){
        for(Candidates candidate:candidateslist){
            try {
                if(candidate.getScore()==candidateEntries.get(index).getScore()){
                    candidate.setInfo("The "+position+" Position goes to you");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private QuixEntry searchForQuestion(int questionNumber,ArrayList<QuixEntry> quixEntries){
        QuixEntry enty=null;
        for(QuixEntry entry:quixEntries)
        {
            if(entry.getIndex()==questionNumber) {
                enty = entry;
                break;
            }
        }
        return  enty;
    }

    private boolean pickedQuestion(){
        return hasNotPickedQuestion==false;
    }
    private void anotherCandidatePick(){

        //this enforces and maintain the codes such that before it can run the candidate must
        //have picked an answer.
        questionNumber=-1;
        hasNotPickedQuestion=true;//this  set the boolean to enable flow in the program
    }

    public void initiateDetails(String ipAddress,String topic,boolean bonusStatus){
        this.ipAddress=ipAddress;
        this.table=topic;
        this.bonusEnabled=bonusStatus;
        setIPAddressLabel(ipAddress);
        setTopicLabel(topic);
        setSessionsMap(topic);//set the map of the id of session with the name
        setSessionQuestion(topic);//set the name of a session with questions in that session
        setInformationLabel("Server Started, candidate can connect ");
        startServer();
    }


    private CandidateEntry searchViewList(String name,String group){
        CandidateEntry resultEntry=null;
        for(CandidateEntry entry:candidateEntries){
            if(entry.getName().equals(name)&&entry.getGroup().equals(group)){
                resultEntry=entry;
                break;
            }
        }
        return resultEntry;
    }



    private void sortViewList(CandidateEntry entry){
        int currentPosition=candidateEntries.indexOf(entry);

        while(currentPosition>0&&entry.getScore()>candidateEntries.get(currentPosition-1).getScore()){
            candidateEntries.set(currentPosition,candidateEntries.get(currentPosition-1));
            currentPosition--;
        }
        candidateEntries.set(currentPosition,entry);
    }

    private Deque<Candidates> sortQualiifiedCandidates(ArrayList<Candidates> qualifiedScoreList){
        //this method can be used to sort a group of candidates list based on the amount of time
        //each of the candidates spent answering questions...this is useful in qualifying candidates
        //for the next round and resolving the issue of candidates with the same score contesting a position
        System.out.println("\n\n\n\t\t\tnQualiFIED\t\t\t\n\n\n");
        ArrayDeque<Candidates> qualifiedCandidatesQueue=new ArrayDeque<>();
        try{
            //The list is sorted in increasing +-order.
            for(int i=1;i<qualifiedScoreList.size();i++) {
                int j=i;
                while (j > 0 && qualifiedScoreList.get(i).totalTime()<qualifiedScoreList.get(j-1).totalTime()) {
                    qualifiedScoreList.set(j,qualifiedScoreList.get(j-1));
                    System.out.println("\n\n---->"+qualifiedScoreList.get(j-1));
                    j--;
                }
                qualifiedScoreList.set(j,qualifiedScoreList.get(i));
                }
            //after the sorting the sorted list is then passed into a queue
            int i=0;
            while(i<qualifiedScoreList.size()){
                qualifiedCandidatesQueue.push(qualifiedScoreList.get(i));
                System.out.print("   "+qualifiedScoreList.get(i).getName()+", Time Spent ---> "+qualifiedScoreList.get(i).totalTime()+"" +
                        "     ");
                i++;
            }


        System.out.print("\n\n\n\nTesting Part");
        int j=0;
            System.out.print(" \n{ ");
        while(j<qualifiedScoreList.size()){
            System.out.print("   "+qualifiedScoreList.get(j).getName()+", Time Spent ---> "+qualifiedScoreList.get(j).totalTime()+"" +
                    "     ");
            j++;
        }
            System.out.print(" }\n ");
       System.out.print("<----->\n\n<----->\n\n");
        }
        catch(RemoteException excp){excp.printStackTrace();}

        return qualifiedCandidatesQueue;
    }


    private void clearAnswer(){
        answer=null;
    }

    private void updateViewList(Candidates candidate){
        try {
            CandidateEntry entry=searchViewList(candidate.getName(),candidate.getGroup());
            entry.setScore(Integer.toString(candidate.getScore()));
            entry.setStatus(candidate.getEligibityStatus());
            sortViewList(entry);
            setTableView(candidateEntries);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void startServer(){
        try {
            TheModerator moderator=new TheModerator();
            String url="rmi://"+ipAddress+"/Moderator";

            Naming.rebind(url,moderator);
            System.out.println("Awesome "+url);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }
    public class TheModerator extends UnicastRemoteObject implements Moderator{

        protected TheModerator() throws RemoteException {
        }

        @Override
        public void RegisterCandidate(Candidates candidate) throws RemoteException {
            candidateslist.add(candidate);
            allcandidateslist.add(candidate);
            candidate.setInfo("You are welcome to Quix");
            CandidateEntry entry=new CandidateEntry(candidate.getName(),candidate.getGroup(),candidate.getEligibityStatus(),candidate.getScore());

            candidateEntries.add(entry);

            setTableView(candidateEntries);
            tableView.setItems(candidateEntries);
        }

        @Override
        public void pickQuestion(int questionno) throws RemoteException {
            questionNumber=questionno;
            hasNotPickedQuestion=false;
            pickQuestionCondition=false;
//            lock.lock();
//            waitCondition1.signal();

            //lock.unlock();

        }

        @Override
        public void answerQuestion(String ans) throws RemoteException {
            answer=ans;
            if(questionType){
              //  lock.lock();
                //questionDelay1.signal();
                answerQuestionCondition=false;
                tellOthersIHaveQuestion();//this stop the timer in other  quix terminals
                //lock.unlock();
            }
            else{
                //lock.lock();
                //bonusDelay1.signal();
                answerBonusQuestion=false;
                tellOthersIHaveQuestion();//this stop the timer in other  quix terminals
                //lock.unlock();
            }
        }


    }

    public class Scores{
        private int scores,population;
        private boolean marked;

        public Scores(int score,int populat){
            this.scores=score;
            this.population=populat;
        }
        public int getScores() {
            return scores;
        }


        public int getPopulation() {
            return population;
        }

        public void increasePopulation() {
            this.population++;
        }

        public void decreasePopulation(){
            this.population--;
        }

        public boolean isMarked() {
            return marked;
        }

        public void setMarked(boolean marked) {
            this.marked = marked;
        }
    }


}
