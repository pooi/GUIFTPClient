import javax.swing.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class FTPClient {

    public static void main(String[] args) {

        try {
            System.setProperty("file.encoding", "UTF-8");
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        }catch (Exception e){
            e.printStackTrace();
        }


        MainFrame frame = new MainFrame("FTPClient");



    }

}