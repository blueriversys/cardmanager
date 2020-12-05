package com.blueriver.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

import com.blueriver.commons.Tools;

class FileEntry {
	public String fileName;
	public int fileSize;
}

public class CardManager {
	private static CardChannel channel;
	private int BLOCK_SIZE = 32;
	private int MAX_FILES_COUNT = 7;
	private int MAX_FILE_NAME_SIZE = 28;

	private boolean fileExists(String fileName) {
		return new File(fileName).exists();
	}
	
	private void connectToTerminal(String terminal) throws CardException {
		TerminalFactory scio = TerminalFactory.getDefault();
		CardTerminal term = scio.terminals().getTerminal(terminal);
		Card card = term.connect("T=0");
		channel = card.getBasicChannel();
	}
	
	private String[] getTerminals() {
		TerminalFactory scio = TerminalFactory.getDefault();
		CardTerminals terminals = scio.terminals();
		java.util.List<CardTerminal> terminalList = null;
		
		try {
			terminalList = terminals.list();
		} catch (CardException e) {
			return new String[]{};
		}
		
		String[] stringArray = new String[terminalList.size()];
		Iterator iterator = terminalList.iterator();
		int i=0;
		while (iterator.hasNext()) {
			CardTerminal term = (CardTerminal)iterator.next();
			stringArray[i] = term.getName();
			i++;
		}
		return stringArray;
	}

	
	private void addFile(String fileName, String cardFileName) {
		if (!cardInited()) {
			System.out.println("card not initialized.");
			return;
		}
		
		if (readTableSector().size() == MAX_FILES_COUNT) {
			System.out.println("Max number of files on card has been reached.");
			return;
		}
		
		if (cardFileName.length() > MAX_FILE_NAME_SIZE) {
			System.out.println("The size of the on card file name is longer than "+MAX_FILE_NAME_SIZE);
			return;
		}
		
		if (isFileFound(cardFileName)) {
			System.out.println("file "+cardFileName + " already stored on card.");
			return;
		}
		
		if ( !fileExists(fileName)) {
			System.out.println("File not found "+fileName);
			return;
		}
		
	    File file = new File(fileName);
	    int fileSize = (int)file.length();
//	    System.out.println("size of file: "+ fileSize);
	    byte fileSizeArray[] = Tools.int32ToByteArrayLE(fileSize);
//	    System.out.println("HEX size of file: "+ Tools.byteArrayToHexString(fileSizeArray));
	    byte dataBlock[] = new byte[32];
	    
	    // initialize data block
	    for (int i=0; i<32; i++) {
	  		dataBlock[i] = ' ';
	    }
	    
	    for (int i=0; i<cardFileName.length(); i++) {
	    	dataBlock[i] = (byte)cardFileName.charAt(i);
	    }
	    
	    for (int i=0; i<4; i++) {
	    	dataBlock[i+28] = fileSizeArray[i]; // 28 is where the file size bytes start
	    }
	    
	    byte[] blockToSend = new byte[33];
	    blockToSend[0] = (byte)dataBlock.length;
	    System.arraycopy(dataBlock, 0, blockToSend, 1, dataBlock.length); // starts on 1, 28 bytes
	    System.arraycopy(fileSizeArray, 0, blockToSend, 29, 4); // starts on 29, 4 bytes

//	    System.out.println("full data block to send: "+ Tools.byteArrayToHexString(blockToSend));

	    // send file name + file size block
		byte[] appData = Tools.executeCommand(channel, (byte)0x80, (byte)0xC2, (byte)0, (byte)0, blockToSend); 
//	      System.out.println("return from C2: "+Tools.byteArrayToHexString(appData));
//        System.out.println("existing page count: "+Tools.byteArrayToInt32(appData));
        
		int ARRAY_SIZE = 32;  
		byte readArray[] = new byte[ARRAY_SIZE];
		byte printArray[] = new byte[ARRAY_SIZE];
		byte inParams[] = new byte[ARRAY_SIZE+1];
		int offset = 0;
		
		InputStream fist = null;
		
		try {
			fist = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		byte writeFlag = 0x00;
		
		while (true ) {
			try {
				int count = fist.read(readArray);
				if (count == -1) {
					break;
				}
				
				// fill with zeros if this is the last block
				if (count < ARRAY_SIZE) {
					int lastBlockSize = ARRAY_SIZE-count; 
					for (int i=0; i<lastBlockSize; i++) {
						readArray[count+i] = 0x00;
					}
					//System.out.println("Last Block "+blockCount + ": " + Tools.byteArrayToHexString(readArray));
					writeFlag = 1;
				}
				else {
					writeFlag = offset == 224 ? (byte)1 : 0;
				}
				
				inParams[0] = (byte)offset;
				System.arraycopy(readArray, 0, inParams, 1, ARRAY_SIZE);
				Tools.executeCommand(channel, (byte)0x80, (byte)0xB3, (byte)0, writeFlag, inParams);
				System.arraycopy(inParams, 1, printArray, 0, printArray.length);
//				System.out.println("Block "+blockCount + ": " + Tools.byteArrayToHexString(printArray));
				
				if (offset == 224) {
					offset = 0;
				} 
				else {
				    // prepare offset for next call
				    offset += ARRAY_SIZE;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	    System.out.println("stored "+fileName + " to card as "+cardFileName);
	}
	
	private void retrieveFile(String fileName, String cardFileName, byte[] password) {
		if (!cardInited()) {
			System.out.println("card not initialized.");
			return;
		}
		
		if (password.length > 15) {
			System.out.println("invalid password.");
			return;
		}
		
		int flag = checkPassword(password);
		
		if (flag == 1) {
			System.out.println("invalid password.");
			return;
		}
		
		if (flag == 2) {
			System.out.println("password has not been set.");
			return;
		}
		
		if (!isFileFound(cardFileName)) {
			System.out.println("file "+cardFileName + " not found on card.");
			return;
		}
		
		byte[] appData = null;
	    
		byte[] nameArray = new byte[MAX_FILE_NAME_SIZE];
		for (int i=0; i<nameArray.length; i++) {
			nameArray[i] = i<cardFileName.length() ?  (byte)cardFileName.charAt(i) : (byte)' ';
		}
		
		// the B5 command ask the card to locate the file and prepare for the subsequent B7 commands.
		// It returns the size of the file on card.
		appData = Tools.executeCommand(channel, (byte)0x80, (byte)0xB5, (byte)0, (byte)0, nameArray);
		int fileSize = Tools.byteArrayToInt32(appData, 0);
		
		// this should not happen here, but will sanity check anyways
		if (fileSize == 0) {
			System.out.println("file not found on card.");
			return;
		}
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		int counter = 0;
		int totalSize = 0;
		int BLOCK_SIZE = 32;
		
		try {
			while (true) {
				// CLA = 0x80, INS = B7 means read the data
				appData = Tools.executeCommand(channel, (byte)0x80, (byte)0xB7, (byte)0, (byte)0, new byte[]{(byte)BLOCK_SIZE}); 
				totalSize += appData.length;
				
				if (totalSize >= fileSize) { // this indicates that there is no more data to read from the card
					int diff = totalSize - fileSize;
					byte[] lastBlock = new byte[BLOCK_SIZE - diff];
					System.arraycopy(appData, 0, lastBlock,	0, lastBlock.length);
//					System.out.println("block " + counter + ": " + Tools.byteArrayToHexString(lastBlock));
					bos.write(lastBlock);
					break;
				}

//				System.out.println("block " + counter + ": " + Tools.byteArrayToHexString(appData));
//				counter++;
				bos.write(appData);
				
				if (totalSize == fileSize) {
					break;
				}
			}
			
			// now let's create the output file
			FileOutputStream fos = new FileOutputStream(fileName);
			bos.writeTo(fos);
			bos.close();
			System.out.println("retrieved into "+fileName+" card file "+cardFileName);
		} 
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
	}

	private void deleteAll(byte[] password) {
		if (!cardInited()) {
			System.out.println("card not initialized.");
			return;
		}
		
		int flag = checkPassword(password);
		
		if (flag == 1) {
			System.out.println("invalid password.");
			return;
		}
		
		// here we prepare the card, read card's table sector
		Tools.executeCommand(channel, (byte)0x80, (byte)0xC3, (byte)0, (byte)0, new byte[]{0x0});
		
		System.out.println("all files deleted from the card.");
	}
	
	
	private void listFiles() {
		if (!cardInited()) {
			System.out.println("card not initialized.");
			return;
		}
		
		List<FileEntry> fileList = readTableSector();
		
		if (fileList.size() == 0) {
			System.out.println("No files found on card.");
			return;
		}
		
		int totalSize = 0;

		for (FileEntry entry : fileList) {
			System.out.println(entry.fileName + ", " + entry.fileSize);
			totalSize += entry.fileSize;
		}
		
		System.out.println("total size "+totalSize);
	}
	
	private void changePassword(byte[] newPassword, byte[] curPassword) {
		if (!cardInited()) {
			System.out.println("card not initialized.");
			return;
		}
		
		int flag = checkPassword(curPassword);
		if (flag == 1) {
			System.out.println("invalid current password.");
			return;
		}
		setPassword(newPassword);
	}
	
	private void initCard(byte[] password) {
		if (password.length > 15) {
			System.out.println("password must be at most 15 characters.");
			return;
		}
		
		byte[] passBytes = new byte[15];
		for (int i=0; i<passBytes.length; i++) {
			passBytes[i] = i<password.length ? password[i] : 0x00;
		}

		int resp = Tools.execCommand(channel, (byte)0x80, (byte)0xC6, (byte)0, (byte)0, passBytes);
		if ( resp == 0x9002) {
			System.out.println("card already initialized.");
			return;
		}
		
		System.out.println("card now initialized.");
	}
	
	private void printTerminals() {
		String[] terms = getTerminals();
		for (String term : terms) {
			System.out.println(term);
		}
	}
	

	
	
	/* methods below are support methods to the main logic above */
	private boolean cardInited() {
		int resp = Tools.execCommand(channel, (byte)0x80, (byte)0xB8, (byte)0, (byte)0, new byte[]{0x0});
		if ( resp == 0x9000) {
			return true;
		}
		return false;
	}
	
	private void setPassword(byte[] password) {
		if (password.length > 15) {
			System.out.println("password must be at most 15 characters.");
			return;
		}
		
		byte[] passBytes = new byte[15];
		for (int i=0; i<passBytes.length; i++) {
			passBytes[i] = i<password.length ? password[i] : 0x00;
		}
		byte[] appData = Tools.executeCommand(channel, (byte)0x80, (byte)0xC5, (byte)0, (byte)0, passBytes);
		System.out.println("password has changed.");
	}
	
	private Map<String, String> getOptions(String arg) {
		Map<String,String> map = new HashMap<>();
		StringTokenizer tokenizer = new StringTokenizer(arg, ",");
		int counter = 0;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			map.put("option"+counter, token);
			counter++;
		}
		return map;
	}

	// 0: password matches
	// 1: password doesn't match
	// 2: password has not been set
	private int checkPassword(byte[] password) {
		byte[] passBytes = new byte[15];
		for (int i=0; i<passBytes.length; i++) {
			passBytes[i] = i<password.length ? password[i] : 0x00;
		}
		byte[] appData = Tools.executeCommand(channel, (byte)0x80, (byte)0xC4, (byte)0, (byte)0, passBytes);
		return (int)appData[0];
	}
	
	// search for a file on the card's table sector
	private List<FileEntry> readTableSector() {
		// here we prepare the card, read card's table sector
		byte[] appData = Tools.executeCommand(channel, (byte)0x80, (byte)0xB8, (byte)0, (byte)0, new byte[]{0x0});
		
		List<FileEntry> entryList = new LinkedList<>();
		
		for (int i=0; i<MAX_FILES_COUNT; i++) {
			appData = Tools.executeCommand(channel, (byte)0x80, (byte)0xB9, (byte)BLOCK_SIZE, (byte)(BLOCK_SIZE*i), new byte[]{0x0});
			byte[] name = new byte[MAX_FILE_NAME_SIZE];
			System.arraycopy(appData, 0, name, 0, name.length);
			String fileOnCard = new String(name).trim();
			
			if (fileOnCard.length() == 0) {
				continue;
			}
			
			FileEntry entry = new FileEntry();
			entry.fileName = fileOnCard;
			entry.fileSize = Tools.byteArrayToInt32(appData, 28); // 28 is the offset of the file size bytes
			entryList.add(entry);
		}
		
		return entryList;
	}
	
	private boolean isFileFound(String fileName) {
		List<FileEntry> fileList = readTableSector();
		
		for (FileEntry entry : fileList) {
			if (entry.fileName.equals(fileName)) {
				return true;
			}
		}
		
		return false;
	}

	private static void printUsage() {
		System.out.println("usage: program <options>");
		System.out.println("Examples:");
		System.out.println("program -init,<password>");
		System.out.println("program -changepass,<newpass>,<curpass>");
		System.out.println("program -add,c:\\myfolder\\file.txt,cardfile");
		System.out.println("program -retrieve,c:\\myfolder\\file.txt,cardfile,<password>");
		System.out.println("program -list");
		System.out.println("program -terminals");
		System.out.println("program -delall,<password>");
	}
	
	public static void main(String[] args) throws Exception {
		CardManager manager = new CardManager();
		manager.connectToTerminal("Blue River V-Card 0");
		
		// Examples of usage:
		//-add,c:\myfiles\file1.txt,myfile1
		//-retrieve,c:\myfiles\file1.txt,myfile1
		//-list
		//-terminals
		
		if (args.length == 0) {
			printUsage();
			System.exit(0);
		}
		
		if (args[0].equals("-h") || args[0].equals("-help")) {
			   printUsage();
			   System.exit(0);
		}
		
		Map<String, String> options = manager.getOptions(args[0]);

		if (args[0].startsWith("-terminals")) {
		   manager.printTerminals();
		   System.exit(0);
		}
		
		if (args[0].startsWith("-delall")) {
			if ( !options.containsKey("option1") ) {
				System.out.println("input param missing.");
				System.exit(0);
			}
		    manager.deleteAll(options.get("option1").getBytes());
		    System.exit(0);
		}
			
		if (args[0].startsWith("-add")) {
			if ( !options.containsKey("option1") || !options.containsKey("option2") ) {
				System.out.println("input params missing.");
				System.exit(0);
			}
			manager.addFile(options.get("option1"), options.get("option2"));
			System.exit(0);
		}
		
		if (args[0].startsWith("-retrieve")) {
			if ( !options.containsKey("option1") || !options.containsKey("option2") || !options.containsKey("option3")) {
				System.out.println("input params missing.");
				System.exit(0);
			}
			manager.retrieveFile(options.get("option1"), options.get("option2"), options.get("option3").getBytes() );
			System.exit(0);
		}

		if (args[0].startsWith("-changepass")) {
			if ( !options.containsKey("option1") || !options.containsKey("option2")) {
				System.out.println("input params missing.");
				System.exit(0);
			}
			manager.changePassword( options.get("option1").getBytes(), options.get("option2").getBytes() );
			System.exit(0);
		}
		
		if (args[0].startsWith("-init")) {
			if ( !options.containsKey("option1") ) {
				System.out.println("input params missing.");
				System.exit(0);
			}
			manager.initCard( options.get("option1").getBytes() );
			System.exit(0);
		}
		
		if (args[0].startsWith("-list")) {
			manager.listFiles();
			System.exit(0);
		}
		
		System.out.println("command unrecognizable");
		System.out.println("");
		printUsage();
	}
	
}
