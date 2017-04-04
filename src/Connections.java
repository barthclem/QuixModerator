import java.io.FileInputStream;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by barthclem on 7/28/16.
 */
public class Connections {
    public static java.sql.Connection getConnection() throws IOException,SQLException {
        FileInputStream input;

        input=new FileInputStream("database.properties");
        Properties prop=new Properties();
        prop.load(input);

        String drivers=prop.getProperty("jdbc.driver");
        if(drivers!=null)
            System.setProperty("jdbc.driver",drivers);

        String dbname=prop.getProperty("jdbc.dbname");
        String username=prop.getProperty("jdbc.dbusername");
        String password=prop.getProperty("jdbc.dbpassword");

        return DriverManager.getConnection(dbname,username,password);

    }
}
