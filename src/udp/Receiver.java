/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package udp;



import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

public class Receiver implements Runnable {
    // ACK loss probability
    public static double LOST_ACK = 0;
int port;
int i;
    public Receiver(int port,int i) {
        this.port = port;
        this.i=i;
    }

    public void run() {
        try {
            int receiverPort = port;
            // Open Socket
            DatagramSocket server_Socket = new DatagramSocket(receiverPort);
            System.out.println("Server Started: Waiting for packets!!");
            Start(server_Socket);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


    }

    void Start(DatagramSocket server_Socket) throws Exception {

try
{

    // Receive Protocol details from the Sender
    byte[] input_Data = new byte[1024];
    DatagramPacket input_DataPacket = new DatagramPacket(input_Data, input_Data.length);
    server_Socket.receive(input_DataPacket);
    String inputDataText = new String(input_DataPacket.getData());

    String[] inputDataArray = inputDataText.split(",");

    String protocol = inputDataArray[0].trim();

    int bitsOfSqeunceNo = Integer.parseInt(inputDataArray[1].trim());
    int WINDOW_SIZE = Integer.parseInt(inputDataArray[2].trim());
    int MSS = Integer.parseInt(inputDataArray[3].trim());

    // Total Sequence No's allowed
    int lastSeqNumber = (int) (Math.pow(2.0, (double) bitsOfSqeunceNo));

    System.out.println("***********************************");
    System.out.println("Receiving Protocol - " + protocol + " From Client "+i);
    System.out.println("***********************************");


    if (protocol.equals("GBN")) {

        // 16 bytes checksum + 1 byte for last packet + 4 bytes for seqNo + MSS
        byte[] receivedPacket = new byte[16 + 1 + 4 + MSS];

        // Data from packets is written in the following output file
        BufferedWriter Write_to_file = new BufferedWriter(new FileWriter("output"+i+".txt"));

        // To detect last packet and stop the server
        boolean end = false;

        // Set initial packet to 0
        int waitingForPacket = 0;

        while (!end) {

            // Receive Packet from Sender
            DatagramPacket receive_Packet_Data = new DatagramPacket(receivedPacket, receivedPacket.length);
            server_Socket.receive(receive_Packet_Data);
            byte[] receiveData = receive_Packet_Data.getData();

            // Convert Packet data to packet object
            RDTPacket packet = new RDTPacket(receiveData, receive_Packet_Data.getLength());

            // Check for valid checksum
            if (packet.isValidPacket()) {

                // Check if received packet is the actual expected packet
                if (waitingForPacket % lastSeqNumber == packet.getSeqNo()) {
                    System.out.println("Received Segment " + packet.getSeqNo() + ";" + " From Client "+i);
                    end = packet.getLast();
                    String text = new String(packet.getData());
                    Write_to_file.write(text);
                    waitingForPacket++;
                } else {
                    System.out.println("Discarded " + packet.getSeqNo() + "; Out of Order Segment Received; Expecting " + waitingForPacket % lastSeqNumber + " Client "+i);
                }
            } else {
                System.out.println("Discarded " + packet.getSeqNo() + "; Checksum Error;"+ " Client "+i);
            }

            // Create and ACK Packet with the sequence number
            RDTAck ackPacket = new RDTAck((waitingForPacket - 1) % lastSeqNumber);
            byte[] ackData = ackPacket.generatePacket();

            // Adding a delay in response to show a clear flow of things
            Thread.sleep(300);

            // Sending ACK for the received packet
            DatagramPacket sendACK = new DatagramPacket(ackData, ackData.length, receive_Packet_Data.getAddress(), receive_Packet_Data.getPort());

            // Indcing a Lost ACK error. This won't actally send ACK but says that ACK was sent
            if (Math.random() > LOST_ACK) {
                server_Socket.send(sendACK);
            } else {
                System.out.println("Lost ACK"+ " From Client "+i);
            }

            // Message to say ACK sent
            System.out.println("ACK Sent: " + ackPacket.getSeqNo()+ " Client "+i);
            System.out.println("");
        }

        Write_to_file.flush();
        Write_to_file.close();

        server_Socket.close();
    } else if (protocol.equals("SR")) {

        // Hashmap to store out of order packets to a buffer
        HashMap<Integer, RDTPacket> receivedPackets = new HashMap<Integer, RDTPacket>();

        // 16 bytes checksum + 1 byte for last packet + 4 bytes for seqNo + MSS
        byte[] receivedPacket = new byte[16 + 1 + 4 + MSS];

        // Data from packets is written in the following output file
        BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));

        // To detect last packet and stop the server
        boolean end = false;

        // Set initial packet to 0
        int waitingForPacket = 0;

        // Window of expecting packets sequence no's. A queue data structure was used as only first received packet can be cleared
        Queue<Integer> WINDOW = new ArrayDeque<>();

        // Initializing window with expecting packets
        for (int i = 0; i < WINDOW_SIZE; i++) {
            WINDOW.add(i);
        }

        while (!end) {

            // Receive Packet from Sender
            DatagramPacket receive_Packet_Data = new DatagramPacket(receivedPacket, receivedPacket.length);
            server_Socket.receive(receive_Packet_Data);
            byte[] receiveData = receive_Packet_Data.getData();

            // Convert Packet data to packet object
            RDTPacket packet = new RDTPacket(receiveData, receive_Packet_Data.getLength());

            // Check for checksum error in the packet
            if (packet.isValidPacket()) {

                System.out.println("Received Segment " + packet.getSeqNo() + ";"+ " From Client "+i);

                ArrayList<Integer> WINDOW_LIST = new ArrayList<Integer>(WINDOW);

                int actualSeqNo = waitingForPacket + WINDOW_LIST.indexOf(packet.getSeqNo());

                // Check if the packet was already received or not. If yes discard.
                if (!receivedPackets.containsKey(actualSeqNo)) {

                    // Store Received packet in buffer
                    receivedPackets.put(actualSeqNo, packet);

                    // Check for out of order packets
                    if (waitingForPacket != actualSeqNo) {
                        System.out.println("Out of Order Segment Received; Stored to Buffer; Expecting " + waitingForPacket % lastSeqNumber+ " Client "+i);
                    }
                } else {
                    // Discard duplicate packets
                    System.out.println("Discarded Segment " + packet.getSeqNo() + ": Duplicate Packet;" + " From Client "+i);
                }


                // Loop to deliver recently packet and also the buffered packets in seq after the received packets
                // Delivers all the packets in sqeuence and clear Window to receive more packets
                while (receivedPackets.containsKey(waitingForPacket)) {
                    RDTPacket bufferedPacket = receivedPackets.get(waitingForPacket);

                    // check if last packet
                    end = bufferedPacket.getLast();
                    String text = new String(bufferedPacket.getData());
                    writer.write(text);
                    System.out.println("Delivered Packet: " + waitingForPacket % lastSeqNumber + " to Client "+i);
                    WINDOW.add((waitingForPacket + WINDOW_SIZE) % lastSeqNumber);
                    waitingForPacket++;
                    WINDOW.remove();
                }

                // Create and ACK Packet with the sequence number
                RDTAck ackPacket = new RDTAck(packet.getSeqNo());
                byte[] ackData = ackPacket.generatePacket();

                // Adding a delay in response to show a clear flow of things
                Thread.sleep(100);

                // Creating an ACK packet for properly received packet
                DatagramPacket sendACK = new DatagramPacket(ackData, ackData.length, receive_Packet_Data.getAddress(), receive_Packet_Data.getPort());

                // Inducing Lost ACK Error. It says ACK sent but it actually won't send the ACK
                if (Math.random() > LOST_ACK) {
                    server_Socket.send(sendACK);
                } else {
                    System.out.println("Lost ACK"+ " Client "+i);
                }

                // ACK message for received packet
                System.out.println("ACK Sent: " + ackPacket.getSeqNo() + " to Client "+i);
                System.out.println("");
            } else {
                System.out.println("Discarded " + packet.getSeqNo() + "; Checksum Error;"+ " Client "+i);
            }

        }

        // close the writer
        writer.flush();
        writer.close();

        // close socket
        server_Socket.close();
    } else if (protocol.equals("SW")) {
        // 16 bytes checksum + 1 byte for last packet + 4 bytes for seqNo + MSS
        byte[] receivedPacket = new byte[16 + 1 + 4 + MSS];

        // Data from packets is written in the following output file
        BufferedWriter writer = new BufferedWriter(new FileWriter("output"+i+".txt"));

        // To detect last packet and stop the server
        boolean end = false;

        // Set initial packet to 0
        int waitingForPacket = 0;

        while (!end) {

            // Receive Packet from Sender
            DatagramPacket receivePacket = new DatagramPacket(receivedPacket, receivedPacket.length);
            server_Socket.receive(receivePacket);
            byte[] receiveData = receivePacket.getData();

            // Convert Packet data to packet object
            RDTPacket packet = new RDTPacket(receiveData, receivePacket.getLength());
            int CheckError = 0;
            // Check for valid checksum
            if (packet.isValidPacket()) {


                System.out.println("Received Segment " + packet.getSeqNo()  + " From Client "+i);
                end = packet.getLast();

                //waitingForPacket++;

            } else {
                System.out.println("Discarded " + packet.getSeqNo() + "; Checksum Error;"+ " From Client "+i);
                CheckError = 1;
            }
            if (CheckError == 0) {
                // Create and ACK Packet with the sequence number
                RDTAck ackPacket = new RDTAck(waitingForPacket % lastSeqNumber);
                byte[] ackData = ackPacket.generatePacket();

                // Adding a delay in response to show a clear flow of things
                Thread.sleep(300);

                // Sending ACK for the received packet
                DatagramPacket sendACK = new DatagramPacket(ackData, ackData.length, receivePacket.getAddress(), receivePacket.getPort());

                // Inducing a Lost ACK error. This won't actally send ACK but says that ACK was sent
                if (Math.random() > LOST_ACK) {
                    server_Socket.send(sendACK);
                    System.out.println("ACK Sent: " + ackPacket.getSeqNo() + " to Client "+i);
                    System.out.println("");
                    String text = new String(packet.getData());
                    writer.write(text);

                    waitingForPacket++;

                } else {
                    System.out.println("Lost ACK" + " Client "+i);
                }
            } else {
                RDTAck ackPacket = new RDTAck(waitingForPacket - 1 % lastSeqNumber);
                byte[] ackData = ackPacket.generatePacket();

                // Adding a delay in response to show a clear flow of things
                Thread.sleep(300);

                // Sending ACK for the received packet
                DatagramPacket sendACK = new DatagramPacket(ackData, ackData.length, receivePacket.getAddress(), receivePacket.getPort());
                server_Socket.send(sendACK);
                System.out.println("ACK Sent: " + ackPacket.getSeqNo() % lastSeqNumber + " to Client "+i);
                System.out.println("");
            }

				/*// Message to say ACK sent
				System.out.println("ACK Sent: " + ackPacket.getSeqNo());
				System.out.println("");*/
        }

        writer.flush();
        writer.close();

        server_Socket.close();

    } else {
        server_Socket.close();
    }
}catch (Exception e1)
{
    System.out.println("hi "+e1.getMessage());

}


    }


}
