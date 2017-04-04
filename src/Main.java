import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;

public class Main extends Application {

    private Stage primaryStage;
    private Connection conn;
    private String ipAddress,portAddress,table;
    private boolean enableBonus;
    private Controller controller;
    @Override
    public void start(Stage primaryStage) throws Exception{
        conn = Connections.getConnection();
        try {
            this.primaryStage = primaryStage;
            FXMLLoader loader=new FXMLLoader(getClass().getResource("moderator.fxml"));
            Parent root = loader.load();
            controller=loader.getController();
            primaryStage.setTitle("Moderator");

            primaryStage.setScene(new Scene(root));
            primaryStage.show();

            displayConnector();
        }
        catch (IOException e){}
    }

    private void displayConnector(){
        try{

            Stage stage=new Stage();

            FXMLLoader loader=new FXMLLoader();
            loader.setLocation(getClass().getResource("connector.fxml"));
            AnchorPane pane=loader.load();
            ConnectorController controller=loader.getController();
            controller.setParameters(stage,this);
            stage.setScene(new Scene(pane));
            stage.setTitle("Connector");
            stage.initOwner(primaryStage);
            stage.showAndWait();
        }
        catch(IOException e){e.printStackTrace();}
    }

    public void initializeParamaters(String ipAddress,String port,String topic,boolean bonusEnabled){
        this.ipAddress=ipAddress;
        this.portAddress=port;
        this.table=topic;
        enableBonus=bonusEnabled;
        controller.initiateDetails(ipAddress,table,bonusEnabled);
    }


    public static void main(String[] args) {
        launch(args);
    }
}