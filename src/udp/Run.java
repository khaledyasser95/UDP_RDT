package udp;

import java.io.FilenameFilter;
import java.net.InetAddress;

public class Run {

    public static void main(String args[]) throws Exception {
        int value =3;
        int port=1000;
        String [] FileName = new String[value];
        String value1="C:\\Users\\Icy\\Documents\\Kolya\\Term 8\\NETWORK\\Final\\UDP _RDT\\Data.txt.txt";
        FileName[0]=value1;
        String value2="C:\\Users\\Icy\\Documents\\Kolya\\Term 8\\NETWORK\\Final\\UDP _RDT\\Data2.txt.txt";
       FileName[1]=value2;
        String value3="C:\\Users\\Icy\\Documents\\Kolya\\Term 8\\NETWORK\\Final\\UDP _RDT\\Data3.txt.txt";
        FileName[2]=value3;

        /**
         * Create a new Thread for Receiver and Sender
         * Each Loop Sends new Port# ,IP# and File
         */
        for(int i=0;i<value;i++)
        {

//             new Thread(new ReadFile(FileName[i])).start();
             new Thread(new Receiver(port,i)).start();
             new Thread(new Sender(port, InetAddress.getByName("127.0.0."+i+1+""),FileName[i],i)).start();

             port++;
              
        }


    }
}
