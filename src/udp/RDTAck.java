/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package udp;


import java.util.Arrays;
import java.nio.ByteBuffer;

// Reliable data transfer ACK packet class to create an ACK packet with sequence no.
public class RDTAck{
	
	private int seqNo;

	// Construtor to create ACK Packet
	public RDTAck(int seqNo) {
		this.seqNo = seqNo;
	}

	// Constructor to decode ACK Packet
	public RDTAck(byte[] packet) {
		ByteBuffer wrapped = ByteBuffer.wrap(packet);
		this.seqNo = wrapped.getInt();
	}

	public int getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(int seqNo) {
		this.seqNo = seqNo;
	}

	// Generate Byte array of ACK packet
	public byte[] generatePacket(){

		byte[] packet = ByteBuffer.allocate(4).putInt(this.seqNo).array();

		return packet;
	}

	@Override
	public String toString() {
		return "RDT Acknowledgement Packet [seq=" + seqNo + "]";
	}

}
