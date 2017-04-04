import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by barthclem on 7/27/16.
 */
public class CreateProperties {
    static FileOutputStream outputStream;
    public static void main(String[]args){

        try{
            outputStream=new FileOutputStream("database.properties");
            Properties prop=new Properties();

            prop.setProperty("jdbc.drivers","com.mysql.jdbc.Driver");
            prop.setProperty("jdbc.dbname","jdbc:mysql://localhost/Quix");
            prop.setProperty("jdbc.dbusername","root");
            prop.setProperty("jdbc.dbpassword","folahan7!");

            prop.store(outputStream,null);
        }
        catch (IOException io){io.printStackTrace();}
        finally {
            if(outputStream!=null){
                try{
                    System.out.print("I'm done writing a properties file");
                outputStream.close();}
                catch(IOException io){io.printStackTrace();}
            }
        }
    }
}
