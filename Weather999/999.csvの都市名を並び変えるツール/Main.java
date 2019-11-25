import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
 
public class Main {
    public static void main(String[] args) throws Exception {
        
        // テキストファイルの内容は”abc”です。
        try (FileInputStream fIStream= new FileInputStream("asia.csv")) {
            InputStreamReader iSReader = new InputStreamReader(fIStream, "UTF-8");
            StringBuffer sb = new StringBuffer();
            int data;
            while ((data = iSReader.read()) != -1) {
                if(data != 44){
                    char c = (char) data;
                    sb.append(c);
                }
                
                    
            }
            
            ArrayList<String> arrayList = new ArrayList<String>();
            for(int i = 0; i < sb.length(); i++){
                
             
                String string = "";
                while(sb.charAt(i) != '\n' && i < sb.length()){
                    string += sb.charAt(i);
                    i++;
                    
                }
                arrayList.add(string);
            }
            
            Collections.sort(arrayList);
            for(int i = 0; i < arrayList.size(); i++){
                System.out.println(arrayList.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
