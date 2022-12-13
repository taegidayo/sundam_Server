import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;

class Check{
    public static final int STX=2;
    public static final int DATATYPE[]={90,91,92};
    public static final int ETX=3;
}
public class Client extends Thread {




    Socket socket = null;

    Client(Socket _socket) {
        this.socket = _socket;
    }
    int DamID=1;



    String errmsg="";
    String separator="|";
    private String info="패킷 형식에 맞춰 입력해 주세요... \n" +
            "패킷 형식: 'STX|종별|패킷길이|ip1|ip2|ip3|ip4|측정년|측정월|측정일|측정시|측정분|측정초|데이터개수(n)|측정데이터*n|ETX'\n" +
            "    타입:  Int|Str| INT   |Int|Int|Int|DD|  Int | Int | Int |  I  | I   |  I |     I      |     I    | I \n" +
            "    예시:  '02|0A|16|0|0|0|1|2022|12|22|07|26|22|1|23|03'\n" +
            "           STX&ETX=02,03 고정, 종별: 0A:조도센서 0B:출입 현황 0C:수위센서 ip:ip1.ip2.ip3.ip4 ";

    Response response = new Response();//this.response메세지의 구성내용들을 넣어둔 클래스


    public void run() {

        try {

            SimpleDateFormat time = new SimpleDateFormat("yyyy/MM/dd/HH:mm:ss");//현재 시간을 출력하기 위한 함수
            OutputStream out = socket.getOutputStream();
            DataOutputStream dout = new DataOutputStream(out);
            InputStream in = socket.getInputStream();
            DataInputStream din = new DataInputStream(in);
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            StringTokenizer st;
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            char[] msg=new char[50];
            //response 메세지 초기화
            String ipAddress = socket.getInetAddress().toString().replace("/", "");

            Connection conn= DriverManager.getConnection(System.getenv("dburl"),System.getenv("dbid"),System.getenv("dbpassword"));
            Statement statement = conn.createStatement();
            while (true) {

                input.read(msg);

                for(int i=0;i<msg.length;i++){
                    if(i==0){
                    System.out.print("받은 패킷:");}
                    System.out.print((int)msg[i]);
                    if(i==msg.length-1){
                        System.out.println();
                    }
                    else System.out.print(',');
                }

                /*todo

               String rMsg = input.readLine();//request메세지
               ArrayList<Integer> list = new ArrayList<Integer>();
               for(int i=0;i<rMsg.length();i++){

                   list.add((int)rMsg.charAt(i));

               }
*/

                //리퀘스트 메시지 확인용


                //request메세지 받아오기
                //만약에 받은 메시지가 공백이거나 Loop(ClientApplication.java)에서 보내는 쓰레기값일 경우 패스
                //todo
//                if (rMsg != null) {
                  if(msg!=null){

                    Request req=new Request(msg,statement,dout);

//                    Request req=new Request(list,statement,dout);


                    if(req.getMessageType()>10) {

                        String[] res = req.check(ipAddress,statement);


                            ResultSet resultSet1 = statement.executeQuery(res[0]);
                            if(req.getDataType()==1||req.getDataType()==2) {


                                ResultSet resultSet2 = statement.executeQuery(res[1]);
                                while (resultSet1.next()) {

                                    System.out.println(resultSet1.getString(1));
                                }
                                while (resultSet2.next()) {

                                    System.out.println(resultSet1.getString(1));
                                }
                            }
                            else {
                                while (resultSet1.next()) {

                                    System.out.println(resultSet1.getString(1));
                                }



                            }


                    }

                    }




                    //this.response메세지 송신
//						dout.writeUTF(this.response.type+"///"+this.response.statecode+"///"+this.response.value+"///"+"END");


                break;
            }
        } catch (SQLException e){
            System.out.println("db접속 실패"+e);

        }

        catch (Exception e) {
            System.out.println();
            System.out.println("예외가 발생했습니다....." + e);

        }

    }
}


class Request {//request 메세지 세부사항

    private int stx;
    private int messageType;
    private int damid;
    private int dataType=0;

    private int[] time;
    private int dataLength;
    private int data=0;
    private int etx;


    int getMessageType(){
        return messageType;
    }
    int getDataType(){
        return dataType;
    }
    int getData(){
return data;
    }


    Request(char[] list,Statement statement,DataOutputStream out) throws IOException {
        int now=0;


        this.stx= (int)list[now++];
        System.out.println("stx:"+this.stx);
        if(this.stx!=2){
            return;
        }
        this.messageType=(int)list[now++];
        System.out.println("메시지 타입:"+this.messageType);
        /*messageType 1= 댐 아이디 구하기
                      2= 댐 리스트에 아이디가 있는 지 확인
                      10> 데이터 삽입
                      else 오류
        */
        if(this.messageType==1){

            try {
                ResultSet ret=statement.executeQuery("Select * from test_h");
                ret.last();
                byte[] outputMessage={0x02,0x02,(byte)(ret.getRow()+1),0x03};
                out.write(outputMessage);

            }
            catch (Exception e){
                byte[] outputMessage={0x02,0x03,0x03};
                out.write(outputMessage);
            }
        }else if(this.messageType==2){
            try {
                int id=(int)list[now++];
                String sql="Select * from test_h where dam_id = " +id;
                System.out.println(sql);
                ResultSet ret=statement.executeQuery(sql);
                ret.last();
                System.out.println(ret.getRow());
                if(ret.getRow()==0){
//                    "INSERT INTO wal (DAM_ID,WATER_LEVEL,MESUR_DT) values ('"+damid+"','"+data+"','"+time[0]+""+time[1]+""+time[2]+""+time[3]+""+time[4]+""+time[5]+"')"
                    sql="INSERT INTO TEST_H (DAM_ID,DAM_NAME) values ('"+id+"','test')";
                    ResultSet res=statement.executeQuery(sql);

                }
                out.write(0x02);
            }
            catch (Exception e){
                byte[] outputMessage={0x02,0x03,0x03};
                System.out.println("에러발생");
                out.write(outputMessage);
            }
        }
        else {
            this.damid=this.messageType/10;
            this.dataType=this.messageType%10;

            this.time = new int[]{
                    ((int)list[now++] - 48) * 1000 + ((int)list[now++] - 48) * 100 + ((int)list[now++] - 48) * 10 + ((int)list[now++] - 48)
                    , ((int)list[now++] - 48) * 10 + ((int)list[now++] - 48)
                    , ((int)list[now++] - 48) * 10 + ((int)list[now++] - 48)
                    , ((int)list[now++] - 48) * 10 + ((int)list[now++] - 48)
                    , ((int)list[now++] - 48) * 10 + ((int)list[now++] - 48)
                    , ((int)list[now++] - 48) * 10 + ((int)list[now++] - 48)};
            System.out.print("시간 값:");
            for (int i = 0; i < this.time.length; i++) {
                System.out.print(this.time[i]);
            }
            System.out.println();
            this.dataLength = (int)list[now++] - 48;
            System.out.println("데이터 길이:"+this.dataLength);
            for (int i = 0; i < this.dataLength; i++) {
                data += ((int)list[now++] - 48) * (int) Math.pow(10, this.dataLength - i - 1);


            }
            System.out.println("데이터값"+this.data);
        }
        this.etx = (int)list[now++];

    }

    Request(ArrayList<Integer> list,Statement statement,DataOutputStream out) throws IOException {
        int now=0;
        this.stx= list.get(now++);
        System.out.println("stx:"+this.stx);
        this.messageType=list.get(now++);
        System.out.println("메시지 타입:"+this.messageType);
        if(this.messageType==1){

        try {
            ResultSet ret=statement.executeQuery("Select * from test_h");

            ret.last();
            System.out.println(ret.getRow()+"요기3");
            byte[] outputMessage={0x02,0x02,(byte)(ret.getRow()+1),0x03};
            out.write(outputMessage);
        }
        catch (Exception e){
            byte[] outputMessage={0x02,0x03,0x03};
            out.write(outputMessage);
        }
        }
        else {
            this.time = new int[]{
                    (list.get(now++) - 48) * 1000 + (list.get(now++) - 48) * 100 + (list.get(now++) - 48) * 10 + (list.get(now++) - 48)
                    , (list.get(now++) - 48) * 10 + (list.get(now++) - 48)
                    , (list.get(now++) - 48) * 10 + (list.get(now++) - 48)
                    , (list.get(now++) - 48) * 10 + (list.get(now++) - 48)
                    , (list.get(now++) - 48) * 10 + (list.get(now++) - 48)
                    , (list.get(now++) - 48) * 10 + (list.get(now++) - 48)};

            for (int i = 0; i < this.time.length; i++) {
                if(i==0)System.out.print("시간값:");
                System.out.print(this.time[i]);
            }
            System.out.println();
            this.dataLength = list.get(now++) - 48;
            System.out.println("데이터 길이:"+this.dataLength);
            for (int i = 0; i < this.dataLength; i++) {
                data += (list.get(now++) - 48) * (int) Math.pow(10, this.dataLength - i - 1);


            }
            System.out.println("데이터 값:"+this.data);
        }
            this.etx = list.get(now++);

    }
    String[] check(String ipAddress,Statement statement){
       if(this.stx!=Check.STX){
           return new String[]{"ERR:STX is Incorrect"};
       }
       else if(this.etx!=Check.ETX){
           return new String[]{"ERR:ETX is Incorrect"};
       }
//       else if(!this.ipAddress.equals(ipAddress)){
//           System.out.println(this.ipAddress+"?"+ipAddress);
//           return  "ERR:ipAddress is Incorrect";
//       }



       switch (this.dataType){
           case 1: return new String[]{"INSERT INTO wal (DAM_ID,WATER_LEVEL,MESUR_DT) values ('" + damid + "','" + data + "','" + time[0] + "" + time[1] + "" + time[2] + "" + time[3] + "" + time[4] + "" + time[5] + "') ",
           "UPDATE test_h SET WATER_LEVEL="+data+",LAST_MESUR='" + time[0] + "" + time[1] + "" + time[2] + "" + time[3] + "" + time[4] + "" + time[5] + "' where DAM_ID = '"+damid+"'"};
           case 2:return new String[]{"INSERT INTO light (DAM_ID,LIGHT_LEVEL,MESUR_DT) values ('" + damid + "','" + data + "','" + time[0] + "" + time[1] + "" + time[2] + "" + time[3] + "" + time[4] + "" + time[5] + "')",
                   "UPDATE test_h SET LIGHT="+data+",LAST_MESUR='" + time[0] + "" + time[1] + "" + time[2] + "" + time[3] + "" + time[4] + "" + time[5] + "' where DAM_ID = '"+damid+"'"};

           case 3: {
               try {
                    String sql="SELECT WORK_NMPR FROM test_h WHERE DAM_ID='"+damid+"'";
                    System.out.println(sql);
                   ResultSet resultSet=statement.executeQuery(sql);
                   resultSet.last();
                   int value=resultSet.getInt(1);
                   if(data==1) {
                       value++;
                   }
                       else {
                           if(data>0)value--;
                   }
                       return new String[]{"UPDATE test_h SET WORK_NMPR=" + (value) + ",LAST_MESUR='" + time[0] + "" + time[1] + "" + time[2] + "" + time[3] + "" + time[4] + "" + time[5] + "' where DAM_ID = '" + damid + "'"};
               } catch (Exception e) {
                   System.out.println(e+"에러 발생");
               }

           }
           default: return new String[]{"ERR: dataType is Incorrect"};
       }


    }
}

class Response {//this.response 메세지 세부사항
    int stx=3;
    int messageType=94;
    int length;
    String ipAddress[];
    String time[];
    int dataCount;
    int data[];
    String etx="03";
}

