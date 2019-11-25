import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import org.apache.commons.io.IOUtils;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Weather999 {

    public static void main(String[] args) throws Exception{

        String str1 = "";                                             //全角の空白をとりのぞきStringの配列に格納 引数に全角空欄にしても大丈夫にする。
        for(int i = 0; i < args.length; i++){
            str1 = str1 + args[i] +",";
        }
        
        String str2 =  str1.replace("　", ",");
        ArrayList<String> array = new ArrayList<String>();
        String aa = "";
        for(int i = 0; i < str2.length(); i++){
            if(str2.charAt(i) == ','){
                array.add(aa);
                aa = "";
            }else{
                aa = aa + str2.charAt(i);
            }
            
        }
        for(int i = 0; i < array.size(); i++){
            if(array.get(i).equals("")){
                array.remove(i);
                i--;
            }
            
           
        }
        
        
        String[] args2 = new String[array.size()];        //全角空白を取り除いた値が格納されている配列
        for(int i = 0; i < array.size(); i++){
            args2[i] = array.get(i);
           
        }


        ArrayList<String> citynameList = new ArrayList<String>();
        int errorCount = 0;
        try {                                                                      //エラーがあったら即終了とせずに、他のエラーもないか調べてあったらエラーメッセージ。
           if(!args2[0].endsWith(".csv")){
                System.out.println("第一引数がcsvファイルではありません。");
                errorCount++;
          
            }else{

                try {

                    citynameList = CSVExtract.makeArrayList(args2[0]);  
                    
                } catch(Exception e) {
                    System.out.println("そのようなCSVファイルはありません"); 
                    errorCount++;
                   
            
                }

            }

            try{
                      int i = 1;
            do{
                if(!args2[i].equals("気温") && !args2[i].equals("風速")  && !args2[i].equals("天気") && !args2[i].equals("気圧") && !args2[i].equals("湿度") && !args2[i].equals("降水量") && !args2[i].equals("国") && !args2[i].equals("日の出") && !args2[i].equals("日の入り")  ){
                    System.out.println("第二引数以降は\"気温\",\"風速\",\"天気\",\"気圧\",\"湿度\",\"降水量\",\"国\",\"日の出\",\"日の入り\"しか指定できません");
                    errorCount++;
                 
                }
                i++;
            }while(i < args2.length);
            }catch(Exception e){
                System.out.println("列名を指定してください");
                errorCount++;
        
            }

        } catch(Exception e) {
            System.out.println("ファイルと列名を指定してください");
            errorCount++;
       
          
        }
        
        if(errorCount > 0){          //エラーが１つでもあれば終了する。
            System.exit(1);
        }



        
        
        
        Display.drawLine(args2);                                       //表の一番上の「-------------」を描く。args2を渡しているのは列名によって「-」の数が違うから
        Display.drawFieldOfCityname();
   



        Column[] column = new Column[args2.length-1];
        for(int i = 1; i < args2.length; i++){                                   //引数の値の数-1個だけ カラム（例：｜  気温    ）を描く
                                                                                        
            column[i-1] = Column.getEnum(args2[i]);
            Display.drawField(column[i-1]);
           
        }

        System.out.println("|");

        Display.drawLine(args2);
       


            ArrayList<City> cityList = new ArrayList<City>();

            for(int i = 0; i < citynameList.size(); i++){


                try{
                  
                     cityList.add(new City(citynameList.get(i)));
                }catch(Exception e){

                  
                }
        
            } 
            

        ArrayList<Column> columArrayList = new ArrayList<Column> ();

        for(int i = 0; i < column.length; i++){
            columArrayList.add(column[i]);
        }

        
        ArrayList<City> sortedcityList = City.sortComplete(cityList, columArrayList);


       
        Display.drawValue(sortedcityList, column);
        


        Display.drawLine(args2);
        System.out.println(City.now);
  
       Display.drawNotFoundCity(Display.notFoundCity);
       Display.drawSameCity(Display.sameCity);
       
   
    
    
    }

    
}


class City  {   //都市を表すクラス
    int ID;
    String name;
    Double temperature;
    double wind;
    String weather;
    int pressure;
    int humidity;
    double precipitation;
    String country;
    int timezone;    //timezonとはロンドンと何秒があるか。ロンドンより進んでいる場合＋ 遅れている場合ー
    int zisa;  //時差（単位： 時間） timezonに3600を割った値
    String sunrise;
    String sunset;
    static String now = "";

    public City(String cityname)throws Exception{        //このコンストラクタでXMLからデータを取得し、インスタンスのフィールドに格納する。ちなみに渡した列名に関わらずすべてのフィールドに格納する。
        this.name = cityname;
        //api.openweathermap.orgに接続しXMLを取得し最終的にInputStream型のインスタンスに格納
        String stringurl = "http://api.openweathermap.org/data/2.5/weather?q=" + cityname + "&mode=xml&units=metric&appid=*****";
        
 

        //受け取ったXMLファイルの一例
        // <current>
        // <city id="1850147" name="Tokyo">
        // <coord lon="139.76" lat="35.68"/>
        // <country>JP</country>
        // <timezone>32400</timezone>
        // <sun rise="2019-11-22T21:24:06" set="2019-11-23T07:30:34"/>
        // </city>
        // <temperature value="13.07" min="12" max="15" unit="celsius"/>
        // <humidity value="93" unit="%"/>
        // <pressure value="1027" unit="hPa"/>
        // <wind>
        // <speed value="5.1" unit="m/s" name="Gentle Breeze"/>
        // <gusts/>
        // <direction value="340" code="NNW" name="North-northwest"/>
        // </wind>
        // <clouds value="75" name="broken clouds"/>
        // <visibility value="3500"/>
        // <precipitation value="3.11" mode="rain" unit="1h"/>
        // <weather number="520" value="light intensity shower rain" icon="09d"/>
        // <lastupdate value="2019-11-23T05:02:35"/>
        // </current> 
        
        
        try{
            URL url = new URL(stringurl);

        
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setDoOutput(true);
        conn.connect();
        PrintWriter out = new PrintWriter(conn.getOutputStream());
        out.flush();
        out.close();
        InputStream in = conn.getInputStream();
            
        Document doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder().parse(in);
        Element current = doc.getDocumentElement();
        Element elementTemperature = findChildByTag(current, "temperature"); 
        this.temperature  = Double.parseDouble(elementTemperature.getAttribute("value"));    //Element型をString型に変換し、またさらにそれをdouble型に変換
      
            
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");


        Element elementCity = findChildByTag(current, "city"); 
        Element elementTimezone= findChildByTag(elementCity, "timezone"); 
        NodeList nodeTimezone = elementTimezone.getChildNodes();
        String stringTimezoneIncludeKigou = nodeTimezone.item(0).toString();
        String stringTimezoneIncludeKuuran = stringTimezoneIncludeKigou.substring(7, stringTimezoneIncludeKigou.length()-1);
        String stringTimezone = stringTimezoneIncludeKuuran.replace(" ", "");
       

        this.timezone = Integer.parseInt(stringTimezone);
        this.zisa = this.timezone / 60 / 60;
      
  
        if(now.equals("")){
            Element elementWeather = findChildByTag(current, "lastupdate"); 
            String stringNow = elementWeather.getAttribute("value");
            Date dateNow = df.parse(stringNow);
            Calendar calNow = Calendar.getInstance();
            calNow.setTime(dateNow);
            calNow.add(Calendar.HOUR_OF_DAY, 9);
            if(calNow.get(Calendar.HOUR_OF_DAY) < 10){
                if(calNow.get(Calendar.MINUTE) < 10){
                    this.now = calNow.get(Calendar.MONTH) + 1 + "月" + calNow.get(Calendar.DAY_OF_MONTH) + "日 0" + calNow.get(Calendar.HOUR_OF_DAY) + ":0" + calNow.get(Calendar.MINUTE) + "現在の情報";
                }else{
                    this.now = calNow.get(Calendar.MONTH) + 1 + "月" + calNow.get(Calendar.DAY_OF_MONTH) + "日 0" + calNow.get(Calendar.HOUR_OF_DAY) + ":" + calNow.get(Calendar.MINUTE)+ "現在の情報";
                }
            }else{
                if(calNow.get(Calendar.MINUTE) < 10){
                    this.now = calNow.get(Calendar.MONTH) + 1 + "月" + calNow.get(Calendar.DAY_OF_MONTH) + "日 " + calNow.get(Calendar.HOUR_OF_DAY) + ":0" + calNow.get(Calendar.MINUTE)+ "現在の情報";
                }else{
                    this.now = calNow.get(Calendar.MONTH) + 1 + "月" + calNow.get(Calendar.DAY_OF_MONTH) + "日 " + calNow.get(Calendar.HOUR_OF_DAY) + ":" + calNow.get(Calendar.MINUTE)+ "現在の情報";
                }
            }
            
        }
        
        Element elementWeather = findChildByTag(current, "weather"); 
        this.weather = elementWeather.getAttribute("value");



        Element elementwind= findChildByTag(current, "wind"); 
        Element elementspeed= findChildByTag(elementwind, "speed"); 
        this.wind = Double.parseDouble(elementspeed.getAttribute("value"));   //Element型をString型に変換し、またさらにそれをdouble型に変換

        

        Element elementCountry = findChildByTag(elementCity, "country"); 
        NodeList nodeCountry = elementCountry.getChildNodes();
        String stringCountry = nodeCountry.item(0).toString();
        this.country = stringCountry.substring(stringCountry.length() - 4, stringCountry.length()-1);




        Element elementSunrise= findChildByTag(elementCity, "sun");
        String sunrise = elementSunrise.getAttribute("rise");
        String sunset = elementSunrise.getAttribute("set");
        

        // コマンド引数でDateオブジェクトを作成
        Date dateSunrise = df.parse(sunrise);
        Date dateSunset = df.parse(sunset);
       
        // Calendarオブジェクトを作成し、上記で作成したDateオブジェクトの日時を設定
        Calendar calSunrise = Calendar.getInstance();
        Calendar calSunset = Calendar.getInstance();
        calSunrise.setTime(dateSunrise);
        calSunset.setTime(dateSunset);
        calSunrise.add(Calendar.HOUR_OF_DAY, this.zisa);
        calSunset.add(Calendar.HOUR_OF_DAY, this.zisa);
        if(calSunrise.get(Calendar.HOUR_OF_DAY) < 10){
            if(calSunrise.get(Calendar.MINUTE) < 10){
                this.sunrise = "0" + calSunrise.get(Calendar.HOUR_OF_DAY) + ":0" + calSunrise.get(Calendar.MINUTE);
            }else{
                this.sunrise = "0" + calSunrise.get(Calendar.HOUR_OF_DAY) + ":" + calSunrise.get(Calendar.MINUTE);
            }
        }else{
            if(calSunrise.get(Calendar.MINUTE) < 10){
                this.sunrise = "" + calSunrise.get(Calendar.HOUR_OF_DAY) + ":0" + calSunrise.get(Calendar.MINUTE);
            }else{
                this.sunrise = "" + calSunrise.get(Calendar.HOUR_OF_DAY) + ":" + calSunrise.get(Calendar.MINUTE);
            }
        }
        


        if(calSunset.get(Calendar.HOUR_OF_DAY) < 10){
            if(calSunset.get(Calendar.MINUTE) < 10){
                this.sunset = "0" + calSunset.get(Calendar.HOUR_OF_DAY) + ":0" + calSunset.get(Calendar.MINUTE);
            }else{
                this.sunset = "0" + calSunset.get(Calendar.HOUR_OF_DAY) + ":" + calSunset.get(Calendar.MINUTE);
            }
        }else{
            if(calSunset.get(Calendar.MINUTE) < 10){
                this.sunset = "" + calSunset.get(Calendar.HOUR_OF_DAY) + ":0" + calSunset.get(Calendar.MINUTE);
            }else{
                this.sunset = "" + calSunset.get(Calendar.HOUR_OF_DAY) + ":" + calSunset.get(Calendar.MINUTE);
            }
        }
        
       



        Element elementPressure = findChildByTag(current, "pressure"); 
        this.pressure = Integer.parseInt(elementPressure.getAttribute("value"));


        Element elementHumidity = findChildByTag(current, "humidity"); 
        this.humidity = Integer.parseInt( elementHumidity.getAttribute("value"));


        Element elementPrecipitation = findChildByTag(current, "precipitation"); 
       
   
        
        
        if(elementPrecipitation.getAttribute("mode").equals("no")){
            this.precipitation = 0;
        }else{
          this.precipitation = Double.parseDouble( elementPrecipitation.getAttribute("value"));
        }
            


        }catch(Exception e){
            Display.notFoundCity.add(cityname);
            throw new Exception();
        }
        
      
    
        
    }
    static Element findChildByTag(Element self, String name)  // Elementクラスのインスタンスの子要素をElement型として返すメソッド
    		throws Exception {
        NodeList children = self.getChildNodes();               //すべての子を取得 
        for(int i = 0; i < children.getLength(); i++) {
            if(children.item(i) instanceof Element) {
                Element e = (Element) children.item(i);
                if(e.getTagName().equals(name)) return e;       // タグ名を照合 
            }
        }
        return null;
    }  
    public String toString(){                                                                                    //toStringを常にオーバーライドする。
        return "name" + this.name + "|temperature:" + this.temperature + "|wind" + this.wind + "|weather" + this.weather + "\n";
    }



    public static void sort(ArrayList<City> cityList, Column column){        //このメソッドでcolumの値によってcityListを並び替えをする。
        if(column.equals(Column.TEMPERATURE)){

        Collections.sort(cityList, new Comparator<City>() {
			
			public int compare(City firstCity, City secondcity) {
 
				return Double.compare(firstCity.temperature, secondcity.temperature);
 
			}
		});
        }else if(column.equals(Column.WINDSPEED)){

            Collections.sort(cityList, new Comparator<City>() {
                
                public int compare(City firstCity, City secondcity) {
     
                    return Double.compare(firstCity.wind, secondcity.wind);
     
                }
            });
        }else if(column.equals(Column.WEATHER)){
            
            Collections.sort(cityList, new Comparator<City>() {
                
                public int compare(City firstCity, City secondcity) {
     
                    return firstCity.weather.compareTo(secondcity.weather);
     
                }
            });
        }else if(column.equals(Column.PRESSURE)){
           
            Collections.sort(cityList, new Comparator<City>() {
                
                public int compare(City firstCity, City secondcity) {
     
                    return Integer.compare(firstCity.pressure, secondcity.pressure);
                   
                }
                 
            });
           
        }else if(column.equals(Column.HUMIDITY)){

            Collections.sort(cityList, new Comparator<City>() {
                
                public int compare(City firstCity, City secondcity) {
     
                    return Integer.compare(firstCity.humidity, secondcity.humidity);
                } 
     
            });

        
       

        }else if(column.equals(Column.PRECIPITATION)){
      
            Collections.sort(cityList, new Comparator<City>() {
                
                public int compare(City firstCity, City secondcity) {
     
                    return Double.compare(firstCity.precipitation, secondcity.precipitation);
     
                }
            });
        }else if(column.equals(Column.COUNTRY)){
            Collections.sort(cityList, new Comparator<City>() {
                
                public int compare(City firstCity, City secondcity) {
     
                    return firstCity.country.compareTo(secondcity.country);
     
                }
            });
        }else if(column.equals(Column.SUMRISE)){

            Collections.sort(cityList, new Comparator<City>() {
                
                public int compare(City firstCity, City secondcity) {
     
                    return firstCity.sunrise.compareTo(secondcity.sunrise);
     
                }
            });
        }else{
            Collections.sort(cityList, new Comparator<City>() {
                
                public int compare(City firstCity, City secondcity) {
     
                    return firstCity.sunset.compareTo(secondcity.sunset);
     
                }
            });
        }
    }
    public static ArrayList<City> sortComplete( ArrayList<City> cityList, ArrayList<Column> mode){                          //一番苦労したところ。再帰を含む。カラムの値によって場合分け
        
        
        //modeとは列名のリストである。modeの最初の値で並び変えられる。
        //まずは実引数のcityList全体の並び替えをして以下のように二次元ArrayListに都市を格納する
        //|都市1|都市2|
        //|都市3|
        //|都市4|都市5|都市6|
        //このとき都市1と都市2のフィールド、都市4と都市5と都市6のフィールドの値は同じである。
        //そしてあらたなcityistを行ごとにつくる。上の例の場合
        //都市1と都市2が含まれるcityList1 都市3が含まれるcityList2,都市4と都市5と都市6が含まれるcityList3がつくられる
        //cityListが2つ以上の場合はこのsortCompleteを再帰的に呼び出す。
        //このときmodeのリストの最初を削除して渡す。(modeの最初の値でもう並び終わったので)
        //最後に二次元のリストを一次元にかえる。
        
        
  
        City.sort(cityList, mode.get(0));
      
        ArrayList<ArrayList<City> > nizigenCity = new ArrayList<ArrayList<City> >();







        if(mode.get(0).equals(Column.TEMPERATURE)){

            ArrayList<Double> numberOfKinds = new ArrayList<Double>();
            for(int i = 0; i < cityList.size(); i++){
                    if(!numberOfKinds.contains(cityList.get(i).temperature)){
                        numberOfKinds.add(cityList.get(i).temperature);
                }
            }
            

            
            int index = 0;

            for(int i = 0; i < numberOfKinds.size(); i++){
                
                ArrayList<Column> mode2 = new ArrayList<Column>();
                for(int j = 0; j < mode.size(); j++){
                    mode2.add(mode.get(j));
                }


                ArrayList<City> eachgyouarray = new ArrayList<City>();    // 一時的なList。 ループの外で初期化される.
                if(eachgyouarray.size() == 0){
                    eachgyouarray.add(cityList.get(index));
                    index++;
              
                }
        
                if(index < cityList.size()){
                                   
                    while(cityList.get(index-1).temperature.equals(cityList.get(index).temperature)){
                        eachgyouarray.add(cityList.get(index));
                        index++;
            
                        if(index == cityList.size()){
                            break;
                        }
                    }
                }

                ArrayList<City> newSortedSameList = new ArrayList<City> ();
                if(mode2.size() > 1){
                    mode2.remove(0);  
                    if( eachgyouarray.size() > 1){
                        newSortedSameList =  City.sortComplete(eachgyouarray, mode2);
                        nizigenCity.add(newSortedSameList);
                        
                            
                    }else{
                        nizigenCity.add(eachgyouarray);
                        
                     
                    }
                }else{
                    nizigenCity.add(eachgyouarray);
                    
                        
                     
                }
                                   
            }
     


         
        }else if(mode.get(0).equals(Column.WINDSPEED)){

 

            ArrayList<Double> numberOfKinds = new ArrayList<Double>();
            for(int i = 0; i < cityList.size(); i++){
                    if(!numberOfKinds.contains(cityList.get(i).wind)){
                        numberOfKinds.add(cityList.get(i).wind);
                }
            }
            

            
            int index = 0;

            for(int i = 0; i < numberOfKinds.size(); i++){
                
                ArrayList<Column> mode2 = new ArrayList<Column>();
                for(int j = 0; j < mode.size(); j++){
                    mode2.add(mode.get(j));
                }


                ArrayList<City> eachgyouarray = new ArrayList<City>();    // 一時的なList。 ループの外で初期化される.
                if(eachgyouarray.size() == 0){
                    eachgyouarray.add(cityList.get(index));
                    index++;
              
                }
        
                if(index < cityList.size()){
                                   
                    while(cityList.get(index-1).wind == cityList.get(index).wind ){
                        eachgyouarray.add(cityList.get(index));
                        index++;
            
                        if(index == cityList.size()){
                            break;
                        }
                    }
                }

                ArrayList<City> newSortedSameList = new ArrayList<City> ();
                if(mode2.size() > 1){
                    mode2.remove(0);  
                    if( eachgyouarray.size() > 1){
                        newSortedSameList =  City.sortComplete(eachgyouarray, mode2);
                        nizigenCity.add(newSortedSameList);
                        
                            
                    }else{
                        nizigenCity.add(eachgyouarray);
                        
                     
                    }
                }else{
                    nizigenCity.add(eachgyouarray);
                    
                        
                     
                }
                                   
            }

        }else if(mode.get(0).equals(Column.WEATHER)){
            
            ArrayList<String> numberOfKinds = new ArrayList<String>();
            for(int i = 0; i < cityList.size(); i++){
                    if(!numberOfKinds.contains(cityList.get(i).weather)){
                        numberOfKinds.add(cityList.get(i).weather);
                }
            }
            

            
            int index = 0;

            for(int i = 0; i < numberOfKinds.size(); i++){
                
                ArrayList<Column> mode2 = new ArrayList<Column>();
                for(int j = 0; j < mode.size(); j++){
                    mode2.add(mode.get(j));
                }


                ArrayList<City> eachgyouarray = new ArrayList<City>();    // 一時的なList。 ループの外で初期化される.
                if(eachgyouarray.size() == 0){
                    eachgyouarray.add(cityList.get(index));
                    index++;
              
                }
        
                if(index < cityList.size()){
                                   
                    while(cityList.get(index-1).weather.equals(cityList.get(index).weather ) ){
                        eachgyouarray.add(cityList.get(index));
                        index++;
            
                        if(index == cityList.size()){
                            break;
                        }
                    }
                }

                ArrayList<City> newSortedSameList = new ArrayList<City> ();
                if(mode2.size() > 1){
                    mode2.remove(0);  
                    if( eachgyouarray.size() > 1){
                        
                        newSortedSameList =  City.sortComplete(eachgyouarray, mode2);
                        nizigenCity.add(newSortedSameList);
                       
                            
                    }else{
                        nizigenCity.add(eachgyouarray);
                        
                     
                    }
                }else{
                    nizigenCity.add(eachgyouarray);
                    
                        
                     
                }
               
 
                    
            }


        }else if(mode.get(0).equals(Column.PRESSURE)){




            ArrayList<Integer> numberOfKinds = new ArrayList<Integer>();
            for(int i = 0; i < cityList.size(); i++){
                    if(!numberOfKinds.contains(cityList.get(i).pressure)){
                        numberOfKinds.add(cityList.get(i).pressure);
                }
            }
            

            
            int index = 0;

            for(int i = 0; i < numberOfKinds.size(); i++){
                
                ArrayList<Column> mode2 = new ArrayList<Column>();
                for(int j = 0; j < mode.size(); j++){
                    mode2.add(mode.get(j));
                }


                ArrayList<City> eachgyouarray = new ArrayList<City>();    // 一時的なList。 ループの外で初期化される.
                if(eachgyouarray.size() == 0){
                    eachgyouarray.add(cityList.get(index));
                    index++;
              
                }
        
                if(index < cityList.size()){
                                   
                    while(cityList.get(index-1).pressure == cityList.get(index).pressure ){
                        eachgyouarray.add(cityList.get(index));
                        index++;
            
                        if(index == cityList.size()){
                            break;
                        }
                    }
                }

                ArrayList<City> newSortedSameList = new ArrayList<City> ();
                if(mode2.size() > 1){
                    mode2.remove(0);  
                    if( eachgyouarray.size() > 1){
                        newSortedSameList =  City.sortComplete(eachgyouarray, mode2);
                        nizigenCity.add(newSortedSameList);
                        
                            
                    }else{
                        nizigenCity.add(eachgyouarray);
                        
                     
                    }
                }else{
                    nizigenCity.add(eachgyouarray);
                    
                        
                     
                }
                                   
            }
        }else if(mode.get(0).equals(Column.HUMIDITY)){



            ArrayList<Integer> numberOfKinds = new ArrayList<Integer>();
            for(int i = 0; i < cityList.size(); i++){
                    if(!numberOfKinds.contains(cityList.get(i).humidity)){
                        numberOfKinds.add(cityList.get(i).humidity);
                }
            }
            

            
            int index = 0;

            for(int i = 0; i < numberOfKinds.size(); i++){
                
                ArrayList<Column> mode2 = new ArrayList<Column>();
                for(int j = 0; j < mode.size(); j++){
                    mode2.add(mode.get(j));
                }


                ArrayList<City> eachgyouarray = new ArrayList<City>();    // 一時的なList。 ループの外で初期化される。フィールドの値が同じCityのリストである
                if(eachgyouarray.size() == 0){
                    eachgyouarray.add(cityList.get(index));
                    index++;
              
                }
        
                if(index < cityList.size()){
                                   
                    while(cityList.get(index-1).humidity == cityList.get(index).humidity ){
                        eachgyouarray.add(cityList.get(index));
                        index++;
            
                        if(index == cityList.size()){
                            break;
                        }
                    }
                }

                ArrayList<City> newSortedSameList = new ArrayList<City> ();
                if(mode2.size() > 1){
                    mode2.remove(0);  
                    if( eachgyouarray.size() > 1){
                        newSortedSameList =  City.sortComplete(eachgyouarray, mode2);
                        nizigenCity.add(newSortedSameList);
                        
                            
                    }else{
                        nizigenCity.add(eachgyouarray);
                        
                     
                    }
                }else{
                    nizigenCity.add(eachgyouarray);
                    
                        
                     
                }
                                   
            }
        }else if(mode.get(0).equals(Column.PRECIPITATION)){



            ArrayList<Double> numberOfKinds = new ArrayList<Double>();
            for(int i = 0; i < cityList.size(); i++){
                    if(!numberOfKinds.contains(cityList.get(i).precipitation)){
                        numberOfKinds.add(cityList.get(i).precipitation);
                }
            }
            

            
            int index = 0;

            for(int i = 0; i < numberOfKinds.size(); i++){
                
                ArrayList<Column> mode2 = new ArrayList<Column>();
                for(int j = 0; j < mode.size(); j++){
                    mode2.add(mode.get(j));
                }


                ArrayList<City> eachgyouarray = new ArrayList<City>();    // 一時的なList。 ループの外で初期化される.
                if(eachgyouarray.size() == 0){
                    eachgyouarray.add(cityList.get(index));
                    index++;
              
                }
        
                if(index < cityList.size()){
                                   
                    while(cityList.get(index-1).precipitation == cityList.get(index).precipitation ){
                        eachgyouarray.add(cityList.get(index));
                        index++;
            
                        if(index == cityList.size()){
                            break;
                        }
                    }
                }

                ArrayList<City> newSortedSameList = new ArrayList<City> ();
                if(mode2.size() > 1){
                    mode2.remove(0);  
                    if( eachgyouarray.size() > 1){
                        newSortedSameList =  City.sortComplete(eachgyouarray, mode2);
                        nizigenCity.add(newSortedSameList);
                        
                            
                    }else{
                        nizigenCity.add(eachgyouarray);
                        
                     
                    }
                }else{
                    nizigenCity.add(eachgyouarray);
                    
                        
                     
                }
                                   
            }
        }else if(mode.get(0).equals(Column.COUNTRY)){


            ArrayList<String> numberOfKinds = new ArrayList<String>();
            for(int i = 0; i < cityList.size(); i++){
                    if(!numberOfKinds.contains(cityList.get(i).country)){
                        numberOfKinds.add(cityList.get(i).country);
                }
            }
            

            
            int index = 0;

            for(int i = 0; i < numberOfKinds.size(); i++){
                
                ArrayList<Column> mode2 = new ArrayList<Column>();
                for(int j = 0; j < mode.size(); j++){
                    mode2.add(mode.get(j));
                }


                ArrayList<City> eachgyouarray = new ArrayList<City>();    // 一時的なList。 ループの外で初期化される.
                if(eachgyouarray.size() == 0){
                    eachgyouarray.add(cityList.get(index));
                    index++;
              
                }
        
                if(index < cityList.size()){
                                   
                    while(cityList.get(index-1).country.equals(cityList.get(index).country)){
                        eachgyouarray.add(cityList.get(index));
                        index++;
            
                        if(index == cityList.size()){
                            break;
                        }
                    }
                }

                ArrayList<City> newSortedSameList = new ArrayList<City> ();
                if(mode2.size() > 1){
                    mode2.remove(0);  
                    if( eachgyouarray.size() > 1){
                        newSortedSameList =  City.sortComplete(eachgyouarray, mode2);
                        nizigenCity.add(newSortedSameList);
                        
                            
                    }else{
                        nizigenCity.add(eachgyouarray);
                        
                     
                    }
                }else{
                    nizigenCity.add(eachgyouarray);
                    
                        
                     
                }
                                   
            }
        }else if(mode.get(0).equals(Column.SUMRISE)){

            ArrayList<String> numberOfKinds = new ArrayList<String>();
            for(int i = 0; i < cityList.size(); i++){
                    if(!numberOfKinds.contains(cityList.get(i).sunrise)){
                        numberOfKinds.add(cityList.get(i).sunrise);
                }
            }
            

            
            int index = 0;

            for(int i = 0; i < numberOfKinds.size(); i++){
                
                ArrayList<Column> mode2 = new ArrayList<Column>();
                for(int j = 0; j < mode.size(); j++){
                    mode2.add(mode.get(j));
                }


                ArrayList<City> eachgyouarray = new ArrayList<City>();    // 一時的なList。 ループの外で初期化される.
                if(eachgyouarray.size() == 0){
                    eachgyouarray.add(cityList.get(index));
                    index++;
              
                }
        
                if(index < cityList.size()){
                                   
                    while(cityList.get(index-1).sunrise.equals(cityList.get(index).sunrise)){
                        eachgyouarray.add(cityList.get(index));
                        index++;
            
                        if(index == cityList.size()){
                            break;
                        }
                    }
                }

                ArrayList<City> newSortedSameList = new ArrayList<City> ();
                if(mode2.size() > 1){
                    mode2.remove(0);  
                    if( eachgyouarray.size() > 1){
                        newSortedSameList =  City.sortComplete(eachgyouarray, mode2);
                        nizigenCity.add(newSortedSameList);
                        
                            
                    }else{
                        nizigenCity.add(eachgyouarray);
                        
                     
                    }
                }else{
                    nizigenCity.add(eachgyouarray);
                    
                        
                     
                }
                                   
            }
        }else{

            ArrayList<String> numberOfKinds = new ArrayList<String>();
            for(int i = 0; i < cityList.size(); i++){
                    if(!numberOfKinds.contains(cityList.get(i).sunset)){
                        numberOfKinds.add(cityList.get(i).sunset);
                }
            }
            

            
            int index = 0;

            for(int i = 0; i < numberOfKinds.size(); i++){
                
                ArrayList<Column> mode2 = new ArrayList<Column>();
                for(int j = 0; j < mode.size(); j++){
                    mode2.add(mode.get(j));
                }


                ArrayList<City> eachgyouarray = new ArrayList<City>();    // 一時的なList。 ループの外で初期化される.
                if(eachgyouarray.size() == 0){
                    eachgyouarray.add(cityList.get(index));
                    index++;
              
                }
        
                if(index < cityList.size()){
                                   
                    while(cityList.get(index-1).sunset.equals(cityList.get(index).sunset)){
                        eachgyouarray.add(cityList.get(index));
                        index++;
            
                        if(index == cityList.size()){
                            break;
                        }
                    }
                }

                ArrayList<City> newSortedSameList = new ArrayList<City> ();
                if(mode2.size() > 1){
                    mode2.remove(0);  
                    if( eachgyouarray.size() > 1){
                        newSortedSameList =  City.sortComplete(eachgyouarray, mode2);
                        nizigenCity.add(newSortedSameList);
                        
                            
                    }else{
                        nizigenCity.add(eachgyouarray);
                        
                     
                    }
                }else{
                    nizigenCity.add(eachgyouarray);
                    
                        
                     
                }
                                   
            }

   
        }

        

        ArrayList<City> completedlist = new ArrayList<City> ();
        for(int i = 0; i < nizigenCity.size(); i++){
            for(int j = 0; j < nizigenCity.get(i).size(); j++){
                completedlist.add(nizigenCity.get(i).get(j));
            }
        }
        
        return completedlist;



    }

}



class CSVExtract{
    public static ArrayList<String> makeArrayList(String filename)throws Exception{
        ArrayList<String> arrayList = new ArrayList<String>();
        try {
            File f = new File(filename);
            BufferedReader br = new BufferedReader(new FileReader(f));
            
            String line;
            // 1行ずつCSVファイルを読み込む
            while ((line = br.readLine()) != null) {
               String[] data = line.split(",", 0); // 行をカンマ区切りで配列に変換
                for(int i = 0; i < data.length; i++){
                    if(arrayList.contains(data[i])){

                        
                        Display.sameCity.add(data[i]);

                      
                    }else{
                        arrayList.add(data[i]);
                    }
                    
                }
              
            }
            br.close();
       
          } catch (IOException e) {
            // Display.drawCannotFindCSVfile();
            throw new Exception();
         
          }
          return arrayList;
          
    }
}



class Display{
    
    public static ArrayList<String> notFoundCity = new ArrayList<String>();
    public static ArrayList<String> sameCity = new ArrayList<String>();
 
    

    public static void drawCannotFindCSVfile(){                             //最初の|   |    都市名   を描く
        System.out.println("そのようなCSVファイルはみつかりませんでした");
        

    }



    public static void drawFieldOfCityname(){                             //最初の|   |    都市名   を描く
        System.out.print("|   ");System.out.print("|       都市名       ");

    }

    public static void drawField(Column column){                     //カラム型をうけとって  「| 風速 」、「|  天気   」、「 |  気温  」 を描く
            
            if(column.equals(Column.TEMPERATURE))System.out.print("|  気温  ");
            if(column.equals(Column.WINDSPEED))System.out.print("| 風速 ");
            if(column.equals(Column.WEATHER))System.out.print("|             天気             ");
            if(column.equals(Column.PRESSURE))System.out.print("| 気圧 ");
            if(column.equals(Column.HUMIDITY))System.out.print("|  湿度 ");
            if(column.equals(Column.PRECIPITATION))System.out.print("|降水量");
            if(column.equals(Column.COUNTRY))System.out.print("|  国  ");
            if(column.equals(Column.SUMRISE))System.out.print("| 日の出 ");
            if(column.equals(Column.SUNSET))System.out.print("|日の入り");
 
    }

    public static void drawValue(ArrayList<City> cityList, Column[] column){        // 「20.56|    broken clouds| 」などの都市名と名前以外の値を描く
   
        for(int i = 0; i < cityList.size(); i++){
                System.out.print("|" + String.format("%3d", i+1) +"|" + String.format("%20s", cityList.get(i).name) + "|" );
               
          
                for(int j = 0; j < column.length; j++){
                    if(column[j].equals(Column.TEMPERATURE)){
                        System.out.print(String.format("%8s", String.format("%.2f", cityList.get(i).temperature)));
                    }
                    if(column[j].equals(Column.WINDSPEED)){
                        System.out.print(String.format("%6s", String.format("%.2f", cityList.get(i).wind)));
                    }
                    if(column[j].equals(Column.WEATHER)){
                        System.out.print(String.format("%30s", cityList.get(i).weather));
                    }
                    if(column[j].equals(Column.PRESSURE)){
                        System.out.print(String.format("%6s", cityList.get(i).pressure));
                    }
                    if(column[j].equals(Column.HUMIDITY)){
                        System.out.print(String.format("%6s%%", cityList.get(i).humidity));
                    }
                    if(column[j].equals(Column.PRECIPITATION)){
                        System.out.print(String.format("%6s", cityList.get(i).precipitation));
                    }
                    if(column[j].equals(Column.COUNTRY)){
                        System.out.print(String.format("%6s", cityList.get(i).country));
                    }
                    if(column[j].equals(Column.SUMRISE)){
                        System.out.print(String.format("%8s", cityList.get(i).sunrise));
                    }
                    if(column[j].equals(Column.SUNSET)){
                        System.out.print(String.format("%8s", cityList.get(i).sunset));
                    }
                    System.out.print("|");
                    
                }
                System.out.println("");

            }

        
    }



    public static void drawLine(String[] args){                               //+-------------------------+   を描くメソッド
        Column[] column = new Column[args.length];
        int lineCount = 24;
        for(int i = 1; i < args.length; i++){
            column[i] = Column.getEnum(args[i]);
            if(column[i] == Column.TEMPERATURE)lineCount += 9;
            if(column[i] == Column.WINDSPEED)lineCount += 7;
            if(column[i] == Column.WEATHER)lineCount += 31;
            if(column[i] == Column.PRESSURE)lineCount += 7;
            if(column[i] == Column.HUMIDITY)lineCount += 8;
            if(column[i] == Column.PRECIPITATION)lineCount += 7;
            if(column[i] == Column.COUNTRY)lineCount += 7;
            if(column[i] == Column.SUMRISE)lineCount += 9;
            if(column[i] == Column.SUNSET)lineCount += 9;
        }

        System.out.print("+");
        for(int i = 0; i < lineCount; i++){
            System.out.print("-");
        }
        System.out.print("+\n");
    }
    public static void drawNotFoundCity(ArrayList notFoundCity){
        int count = 0;
        for(int i = 0; i < notFoundCity.size(); i++){
            if(!notFoundCity.get(i).equals("")){
                System.out.print("\"" + notFoundCity.get(i) + "\" ");
                count++;
            }
        }
        if(notFoundCity.size() > 0 && count > 0){
            System.out.println("という都市はみつかりませんでした。");
        }
   
    }
    public static void drawSameCity(ArrayList sameCity){
        int count = 0;
        for(int i = 0; i < sameCity.size(); i++){
            if(!sameCity.get(i).equals("")){
                count++;
            }
        }
        if(sameCity.size() > 0 && count > 0){
            System.out.print("CSVファイルに同じ名前が入っていました    値：");
        }
        for(int i = 0; i < sameCity.size(); i++){

            if(!sameCity.get(i).equals("")){
                System.out.print("\"" + sameCity.get(i) + "\", ");
            }



            
        }
        if(sameCity.size() > 0){
            System.out.println("");
        }
   
    }

 
}



enum Column{                                //表の列を表す。
 
    TEMPERATURE("気温"),       
    WINDSPEED("風速"),
    WEATHER("天気"),
    PRESSURE("気圧"),
    HUMIDITY("湿度"),
    PRECIPITATION("降水量"),
    COUNTRY("国"),
    SUMRISE("日の出"),
    SUNSET("日の入り");         
    private String name;

    public String getName() {
        return name;
    }


    private Column (String name) {
      this.name = name;
    }

    // メソッドのオーバーライド
    public String toString() {
      return name;
    }


    public static Column getEnum(String str) {
        // enum型全てを取得します。
        Column[] enumArray = Column.values();

        // 取得出来たenum型分ループします。
        for(Column enumStr : enumArray) {
            // 引数とenum型の文字列部分を比較します。
            if (str.equals(enumStr.name.toString())){
                return enumStr;
            }
        }
        return null;
    }
}


