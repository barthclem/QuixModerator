import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Created by barthclem on 8/3/16.
 */
public class ConnectorController {
    @FXML
    private TextField IpAddressField;
    @FXML
    private TextField PortAddressField;
    @FXML
    private CheckBox  bonusCheck;
    @FXML
    private ComboBox<String> topics;


    private boolean bonusEnabled;
    private String ipAddress;
    private String port,topic;
    private Stage stage;
    private Main mainApp;

    private Connection conn;

    public void setParameters(Stage stage,Main mainApp){
        this.stage=stage;
        this.mainApp=mainApp;

    }
    @FXML
    private void initialize(){
        try {
            conn=Connections.getConnection();
            IpAddressField.setText("127.0.0.1");
            PortAddressField.setText("8000");
            loadTopics();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        catch (IOException e){e.printStackTrace();}
    }
    @FXML
    private void chooseTopic(){
        topic=topics.getValue();
    }

    @FXML
    private void fixBonus(){
        bonusEnabled=bonusCheck.isSelected();
    }

    @FXML
    private void connect(){
        bonusEnabled=bonusCheck.isSelected();
        if(inputIsValid()){
            mainApp.initializeParamaters(ipAddress,port,topic,bonusEnabled);
            stage.close();
        }
    }
    private void loadTopics() throws SQLException {
        DatabaseMetaData metaData=conn.getMetaData();
        ResultSet resultSet=metaData.getTables(null,null,null,new String[]{"TABLE"});
        ObservableList<String> list= FXCollections.observableArrayList();
        while(resultSet.next()){
            String ini=resultSet.getString("TABLE_NAME");
            if(!ini.matches(".*Sessions.*")){
                list.add(ini);
            }
        }
        topics.setItems(list);
        conn.close();
    }

    private boolean inputIsValid(){
        String errorMessage="";
        String patterns="(([01]?[0-9]{1,2}\\.)|([2](([0-4][0-9])|([5][0-5]))\\.)){3}(([01]?[0-9]{1,2})|([2](([0-4][0-9])|([5][0-5]))12))";
        String PortPattern="([1][0][5-9][0-9]|([2-9][0-9]{3}))";
        ipAddress=IpAddressField.getText().trim();
        port=PortAddressField.getText().trim();

        if(IpAddressField.getText().trim().length()==0||IpAddressField.getText()==null)
        {
            errorMessage += "\nIp Address cannot be empty";
        }
        if(IpAddressField.getText()!=null)
        {
            try{

                if(!Pattern.matches(patterns,IpAddressField.getText().trim()))
                    errorMessage+="\n you have entered an invalid ip address.\n Example of Ip Address is : 127.0.0.1";
            }
            catch(Exception e){}
        }

        if(PortAddressField.getText().trim().length()==0||PortAddressField.getText()==null)
        {errorMessage += "\n Port Address cannot be left empty";;}
        if(!Pattern.matches(PortPattern,PortAddressField.getText().trim()))
            errorMessage+="\n you have entered an invalid port address .\n Example of valid port address \n must be in the range 1047-9999";
        if(errorMessage.trim().length()!=0){
            Alert errorAlert=new Alert(Alert.AlertType.ERROR);
            errorAlert.initOwner(stage);
            errorAlert.setHeaderText("Error Message");
            errorAlert.setTitle("Input Error");
            errorAlert.setContentText(errorMessage);
            errorAlert.showAndWait();
            return false;
        }
        else{
            return true;
        }
    }





}
