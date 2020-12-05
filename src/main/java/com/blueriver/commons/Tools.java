package com.blueriver.commons;

import java.io.UnsupportedEncodingException;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class Tools {
	/**
	 * This method sends the SELECT FILE command to the card
	 */
	public static byte[] selectFile(CardChannel channel, byte[] appId) { 
		return executeCommand(channel, (byte)0x00, (byte)0xA4, (byte)0, (byte)0, appId);
	}
	
	/**
	 * This method sends the DESELECT FILE (deselect app) command to the card
	 */
	public static byte[] deselectFile(CardChannel channel) { 
		return executeCommand(channel, (byte)0x00, (byte)0xA5, (byte)0, (byte)0, new byte[]{});
	}
	
	/**
	 * This method sends a generic command to the card
	 */
	public static byte[] executeCommand(CardChannel channel, byte cla, byte ins, byte p1, byte p2, byte[] inParams) { 
		ResponseAPDU resApdu = null;
		
		try{
            CommandAPDU apdu = new CommandAPDU(cla, ins, p1, p2, inParams);
            resApdu = channel.transmit(apdu);
        }
        catch(CardException ex){
            System.out.println("Exception transmitting APDU: "+ex.getMessage());
            return null;
        }
		
        /*
         *  TODO
         *  we have to better treat when application was not found in the card
        */
		return resApdu.getData();
	}
	
	// used for when we're only interested in the response bytes (90 00)
	public static int execCommand(CardChannel channel, byte cla, byte ins, byte p1, byte p2, byte[] inParams) { 
		ResponseAPDU resApdu = null;
		
		try {
            CommandAPDU apdu = new CommandAPDU(cla, ins, p1, p2, inParams);
            resApdu = channel.transmit(apdu);
    		return resApdu.getSW();
        }
        catch(CardException ex){
            System.out.println("Exception transmitting APDU: "+ex.getMessage());
            return 0;
        }
	}
	
	public static String byteArrayToHexString(byte[] data) {
		return byteArrayToHexString(data, 0);
	}
	
	public static String byteArrayToHexString(byte[] data, int offset) {
        final byte[] HEX_CHAR_TABLE = {
			    (byte)'0', (byte)'1', (byte)'2', (byte)'3',
			    (byte)'4', (byte)'5', (byte)'6', (byte)'7',
			    (byte)'8', (byte)'9', (byte)'A', (byte)'B',
			    (byte)'C', (byte)'D', (byte)'E', (byte)'F'
		};    

        byte[] localData;
        
        if (offset != 0) {
        	localData = new byte[data.length - offset];
        	System.arraycopy(data, offset, localData, 0, data.length-offset);
        }
        else {
        	localData = data;
        }
        
	    byte[] hex = new byte[3 * localData.length];
	    int index = 0;

	    for (byte b : localData) {
	      int v = b & 0xFF;
	      hex[index++] = HEX_CHAR_TABLE[v >>> 4];
	      hex[index++] = HEX_CHAR_TABLE[v & 0xF];
	      hex[index++] = ' ';
	    }
	    
	    String str = null;
	    try {
			str = new String(hex, "ASCII");
		} 
	    catch (UnsupportedEncodingException e) {}	
	    return str;
	}
	
	// Big Endian
	public static byte[] int32ToByteArrayBE(int val)
	{
		byte pBytes[] = new byte[4];
		pBytes[0] = (byte)val;
		pBytes[1] = (byte)(val >> 8);
		pBytes[2] = (byte)(val >> 16);
		pBytes[3] = (byte)(val >> 24);
		return pBytes;
	}
  
	// Little Endian
	public static byte[] int32ToByteArrayLE(int val)
	{
		byte pBytes[] = new byte[4];
		pBytes[0] = (byte)(val >> 24);
		pBytes[1] = (byte)(val >> 16);
		pBytes[2] = (byte)(val >> 8);
		pBytes[3] = (byte)val;
		return pBytes;
	}
  
	// Little Endian
	public static byte[] int16ToByteArrayLE(short val)
	{
		byte pBytes[] = new byte[2];
		pBytes[0] = (byte)(val >> 8);
		pBytes[1] = (byte)val;
		return pBytes;
	}
  
	public static int byteArrayToInt32(byte[] b) {
		return byteArrayToInt32(b, 0);
	}
	
    /**
     * Convert the first 4 digits of a byte array in LITTLE ENDIAN format to an int.
     *
     * @param b The byte array
     * @param offset Start of the byte array
     * @return The integer
     */
    public static int byteArrayToInt32(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (3 - i) * 8;
            value += (b[i+offset] & 0x000000FF) << shift;
        }
        return value;
    }  

}
