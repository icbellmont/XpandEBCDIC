import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

import javax.swing.JOptionPane;

/**
 * Esta clase convierte ficheros con formato EBCDIC de IBM (charset CP1047) a formato ASCI (charset ISO-8859-1)
 * Toma como Argumento de entrada el fichero de formato en texto plano, con la siguiente estructura:
 * 
 * Format File:
 * -- Tipo de Registro 0; un solo registro Tipo 0 con la siguiente estructura, con los valores separados por ";": 
 *   
 *   0;FileName;RegLength
 *   0 Tipo de registro
 * ... Nombre del fichero a convertir en EBCDIC
 * ........... Longitud de registro
 * 
 * eg. 
 * 0;C:\Users\xxxxxx\Documents\_WK\TESTING\f0.txt;98
 *
 * -- Tipo de Registro 1; un registro por cada campo (lógico) a considerar en cada registro,
 *  con la siguiente estructura, con los valores separados por ";": 
 *   
 *   1;FieldName;Picture;Type;IniPos;Length;ASCIILength;OutMask *   
 *   1 Tipo de registro de definicion de campo
 *   ... Nombre de Campo
 *   ............ COBOL Picture (solo informativo)
 *   ......................Tipo de campo:
 *   ...................... C  COBOL USAGE COMP (Binary (COMP) items) 
 *   ...................... X  COBOL USAGE DISPLAY
 *   ...................... P  Packed-decimal (COMP-3) items
 *   ........................... Posición Inicial (dentro del registro)
 *   ............................... Longitud de campo (en formato EBCDIC)
 *   ..................................... Longitud de Salida (en formato ASCII)
 *   .......................................... Máscara Formatter fmt.format(sOutMask, sData).toString();
 *  
 * eg. 
 * 1;CODENTID;X(002);X;0;2;2;%2s
 * 1;CODCONVE;X(008);X;2;8;8;%8s
 * 1;BS4003U.;X(008);X;10;8;8;%8s
 * 1;ANOACUERDO;S9(4)COMP;C;18;2;4;%04d    
 * 1;PORMARGEN;S9(03)V9(06)COMP-3;P;86;5;9;%09d 
 *  
 * La clase genera un fichero plano de salida en ASCII (charset ISO-8859-1) con el mismo nombre que el fichero de entrada
 * (especificado en el Registro 0 del fichero de Formato (FileName)) añadiendo "_out.txt".
 * ie. Si Reg.0 = "0;C:\Users\xxxxxx\Documents\_WK\TESTING\f0.txt;98", generará el fichero desempaquetado como
 * C:\Users\xxxxxx\Documents\_WK\TESTING\f0.txt_out.txt 
 * Desempaqueta campos COBOL definidos como COMP y COMP-3.
 * Adicionalmente se genera como salida un fichero de estadísticas con formato "SorceFileName_stats_TIMESTAMP.txt",
 * indicando el número de registros leido y transformado. 
 * eg. FileName;f0.txt;Rows;0  
 *
 *  
 * @author: IcBellmónt 
 * @version: 05/12/2018/Initial
 * @see <a href = "https://www.ibm.com/support/knowledgecenter/SSQ2R2_14.0.0/com.ibm.ent.cbl.zos.doc/PGandLR/ref/rlddecom.html" > www.ibm.com/support </a>
 *
 */

public class XpandEBCDIC {

private static String FILENAME = "X:\\TEMP\\bohay.txt";
private static int REGLENGTH = 630;
static ArrayList <String> listFormat = new ArrayList<String>();
static ArrayList <String> listError = new ArrayList<String>();

public static void main(String[] args) {

	long lStart = Calendar.getInstance().getTimeInMillis();
	String sTS = (getCurrentTimeStamp());
	
	try
	{
		FileOutputStream fout= new FileOutputStream("XpandEBCDIC_stdout_" + sTS  + ".log");
		FileOutputStream ferr= new FileOutputStream("XpandEBCDIC_stderr_" + sTS  + ".log");
		
		MultiOutputStream multiOut= new MultiOutputStream(System.out, fout);
		MultiOutputStream multiErr= new MultiOutputStream(System.err, ferr);
		
		PrintStream stdout= new PrintStream(multiOut);
		PrintStream stderr= new PrintStream(multiErr);
		
		System.setOut(stdout);
		System.setErr(stderr);
	}
	catch (FileNotFoundException ex)
	{
		ex.printStackTrace();
		System.out.println("> ERROR redirecting outputs !!");
		System.exit(99);
	}
	
	System.out.println("\nXPandEBCDIC started..." + lStart + "\n");
	
//	String sFmtFile = JOptionPane.showInputDialog(
//    		null, 
//            "Enter the Format file", 
//            "Xpand format file", 
//            JOptionPane.QUESTION_MESSAGE
//            );
	
	String sFmtFile = "C:\\Users\\xxxxxx\\Documents\\_WK\\RSI\\SAREB\\CAPRI\\MIGRACION\\unpack\\TESTING\\file1_txt_fmt.txt";
	sFmtFile = "C:\\Users\\xxxxxxx\\Documents\\_WK\\RSI\\SAREB\\CAPRI\\MIGRACION\\unpack\\TESTING\\filef32_fmt.txt";
	sFmtFile = "C:s\\Users\\xxxxxxx\\Documents\\_WK\\RSI\\SAREB\\CAPRI\\MIGRACION\\unpack\\TESTING\\FMT_F33.txt";
	sFmtFile = "C:\\Users\\xxxxxxx\\Documents\\_WK\\RSI\\SAREB\\CAPRI\\MIGRACION\\unpack\\TESTING\\FMT_F6.txt";
	sFmtFile = "C:\\Users\\xxxxxxx\\Documents\\_WK\\RSI\\SAREB\\CAPRI\\MIGRACION\\unpack\\TESTING\\FMT\\FMT_FXXXX50.txt";
	
	String sArg1 = ""; 
	
	try {
		sArg1 = args[0];
	} catch (Exception e2) {
		e2.printStackTrace();
	}
		
	if (sArg1.isEmpty() || sArg1.length() <= 3) {
		System.out.println("> NO Argument Error!!");
		System.exit(99);
	}
	
	sFmtFile = sArg1;
	
	ReadFormatFile(sFmtFile);
	ValidateListFormat();
	
	BufferedWriter bw = null;
	FileWriter fw = null;
			
//	ArrayList <String> list = new ArrayList<String>();
	
	System.out.println("Reading " + FILENAME);
	FileInputStream is = null;
	try {
		is = new FileInputStream(FILENAME);
	} catch (FileNotFoundException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	};
	
	//try (BufferedReader br = new BufferedReader(new FileReader(FILENAME))) {
	
	try (InputStreamReader isr = new InputStreamReader(is);) {

		fw = new FileWriter(FILENAME + "_out.txt");
		bw = new BufferedWriter(fw);

		String sCurrentLine;
		String sCurrentOutLine;
		Long   lRegs = (long) 0;
		byte[] bReg = new byte[REGLENGTH];
		int ReadBytes = 0;

		while ((ReadBytes = is.read(bReg)) != -1) {
			
			sCurrentLine = GetHexString(bReg);
			sCurrentLine = bReg.toString();
			//System.out.println("sCurrentLine::" + BytesToHex(sCurrentLine.getBytes()));
			//System.out.println("read bReg::" + BytesToHex(bReg));
			lRegs = lRegs + 1;
			//ValidateReg(sCurrentLine, lRegs);
			sCurrentOutLine = ValidateReg(bReg, lRegs);
			
//			list.add(sCurrentLine);
			 
			String sReg = sCurrentLine.substring(0,2);
			//System.out.println("Reg:" + sReg);
			
			if (sReg.equals("**")) {  
				bw.write(sCurrentLine); bw.newLine();};
			
			bw.write(sCurrentOutLine); bw.newLine();
			
			//System.out.println("+WRITTING sCurrentOutLine to " + FILENAME+"_out.txt " + sCurrentOutLine);
		};
		
		System.out.println("Total Registros:" + lRegs);
		
		bw.flush();
		
		PrintListError();
		WriteStatFile(lRegs);
		
		// Getting the size of the list
//		int size = list.size();
//		System.out.println("The size of the list is: " + size);
//		list.clear();
		
		long lEnd = Calendar.getInstance().getTimeInMillis();
		System.out.println("\nStarting at: " + lStart);
		System.out.println("Ending at:   " + lEnd);
		System.out.println("Elapsed Seconds: " + ((lEnd - lStart)*0.001) + " Mins: " + (((lEnd - lStart)*0.001) / 60));
		System.exit(0);

	} catch (IOException e) {
		e.printStackTrace();
		System.out.println("caught exception: " + e);
	} finally {

		try {

			if (bw != null)
				bw.close();

			if (fw != null)
				fw.close();

		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println("caught exception: " + ex);
		};
	};
}


public static void ReadFormatFile(String sFmtFile) {
			
	try (BufferedReader br = new BufferedReader(new FileReader(sFmtFile))) {

		String sCurrentLine;
		Long lRegs = (long) 0;

		while ((sCurrentLine = br.readLine()) != null) {
			System.out.println(sCurrentLine);
			lRegs = lRegs + 1;				
			listFormat.add(sCurrentLine);				
		};
		
		System.out.println("Registros:" + lRegs);
		int size = listFormat.size();
		//System.out.println("The size of the list is: " + size);
		
		if (size <= 0) {
			JOptionPane.showMessageDialog(null, 
		    		"Empty ListFormat!.\n", 
	                "ERROR MESSAGGE", 
	                JOptionPane.ERROR_MESSAGE);
			System.exit(99);
		}
		 
	} catch (IOException e) {
		e.printStackTrace();
		System.out.println("caught exception: " + e);
		System.exit(99);
	} finally {
	};
}


public static void ValidateListFormat() {
				
	for(String sReg : listFormat){
		System.out.println("ValidateListFormat*:" + sReg);
		String[] ListFields = sReg.split(";");
		String sTipReg = ListFields[0];
		//System.out.println("TipReg: " + sTipReg);
		
		switch (sTipReg) {
          case "0":
        	 ValidateReg0(sReg);
             break;
          case "1":
        	 ValidateReg1(sReg);
             break;	         
          default:
             throw new IllegalArgumentException("Invalid sTipReg ");
        };
					
		for(String Field : ListFields){
			//System.out.println(Field);					
		};
	};		 
}


public static void ValidateReg0(String sReg) {
	
	//0;FileName;RegLength
	
	String[] ListFields = sReg.split(";");
	String   sTipReg = ListFields[0];
	String   sFile = ListFields[1];
	Integer  iRegLength = Integer.parseInt(ListFields[2]);
	
	//System.out.println("FileName: " + sFile);
	FILENAME = sFile;
	REGLENGTH = iRegLength;
	
	if (FILENAME.isEmpty()) {
		throw new IllegalArgumentException("Invalid Input FileName in Reg.0! ");			 
		//System.exit(99);
	};
	
}


public static void ValidateReg1(String sReg) {
	
	//1;FieldName;Picture;Type;IniPos;Length;ASCIILength;OutMask 
	
	String[] ListFields = sReg.split(";");
	String   sTipReg = ListFields[0];
	String   sFieldName = ListFields[1];	 
	String   sPic = ListFields[2];
	String   sType = ListFields[3];
	Integer  iIniPos = Integer.parseInt(ListFields[4]);
	Integer  iLength = Integer.parseInt(ListFields[5]);
	Integer  iASCIILength = Integer.parseInt(ListFields[6]);
	String   sOutMask = ListFields[7];
 
    String  sFieldData = "";
	
	switch (sType) {
      case "A":
      case "N":	  
      case "D":	
      case "C":	
      case "X":	
      case "P":	  
  	    break;                
    default:
       throw new IllegalArgumentException("Invalid sFieldType ");
    };
	
	if (iLength <= 0) {
		throw new IllegalArgumentException("Invalid iField Length ");			 
		//System.exit(99);
	};
	
}


public static String ValidateReg(byte[] bReadReg, long lNReg) {	
	
	//xxxxxxxxxxxxx
	
	String sReg2Write = "";
	
	Formatter fmt;
	StringBuilder sbuf = new StringBuilder();
	
	for(String sRule : listFormat){
		
		String[] ListFields = sRule.split(";");
		String  sTipReg = ListFields[0];
		
		if (sTipReg.equals("1")) {  
			String   sFieldName = ListFields[1];	 
			String   sPic = ListFields[2];
			String   sType = ListFields[3];
			Integer  iIniPos = Integer.parseInt(ListFields[4]);
			Integer  iLength = Integer.parseInt(ListFields[5]);
			Integer  iASCIILength = Integer.parseInt(ListFields[6]);
			String   sOutMask = ListFields[7];
		    
			String   sFieldData = "";
		    Boolean  bLengthErr = true;
		    byte[]   bData = new byte[iLength];
		    
		    try {
		        //String sData = sReadReg.substring(iIniPos,iIniPos+iLength);
		        //byte[] sData = sReadReg.substring(iIniPos,iIniPos+iLength).getBytes();
		        //byte[] sData = bReadReg.substring(iIniPos,iIniPos+iLength).getBytes();		    	
		    	//byte[] sData = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};   	
		    	 
		        System.arraycopy(bReadReg, iIniPos, bData, 0, iLength);
                bLengthErr = false;
                //sFieldData = sData.toString();
                //System.out.println("Reg:" + lNReg + ">>>>>>>>>>>> sData: Bytes2Hex:" + BytesToHex(bData));
			 
			    } catch (Exception e) {
				e.printStackTrace();
				System.out.println("caught exception: " + e);
				bLengthErr = true;
				sFieldData = "error!";
		    };  
		    
		    if (bLengthErr) {  
		    	listError.add(" Input Register: " + lNReg
		    			    + " Length Error at Field: " + sFieldName);	
		    };
		    
		    if (sType.equals("C")) {  
		    	
		    	sbuf.delete(0, sbuf.capacity());
		    	fmt = new Formatter(sbuf);
		        //fmt.format("%09d", new BigInteger(sData).intValue());
		        fmt.format(sOutMask, new BigInteger(bData).intValue());
		        
		    	sFieldData = sbuf.toString();;
		         
		    };
		    
		    if (sType.equals("X")) {  
		    	
		    	//System.out.println(" xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx   Cleanx00 es: " + CleanX00 (bData)) ; 
		    	bData = CleanX00 (bData, sFieldName);		    	
		        String sCadena1 = new String(bData);		
		        
		        String sCadena2 = Convert(sCadena1,"CP1047","ISO-8859-1");
		        
		        sbuf.delete(0, sbuf.capacity());		    
		    	fmt = new Formatter(sbuf);
		    	sFieldData = fmt.format(sOutMask, sCadena2).toString();
		        		         
		        sFieldData = sFieldData.substring(0, iLength);
		        //System.out.println(" sFieldData AJ: " + sFieldData + " Bytes2Hex:" + BytesToHex(sFieldData.getBytes()));		         
		    };
		    
		    if (sType.equals("P")) {  
	    			    	
		    	String sUnpackedPD = unpackPD (bData,0);
		        String sCadena1 = new String(sUnpackedPD);	        
		        //System.out.println("............................................................... UNPACKED PD: " + sCadena1 + " Bytes2Hex:" + BytesToHex(sCadena1.getBytes()));
		        
		        sbuf.delete(0, sbuf.capacity());		    
		    	fmt = new Formatter(sbuf);
		    	//sFieldData = fmt.format(sOutMask, Long.valueOf(sCadena1).longValue());
		    	
		    	long l = Long.parseLong(sCadena1);
		    	String strLong = Long.toString(l);
		    	//System.out.println("............................................................... strLong PD: " + strLong + " Bytes2Hex:" + BytesToHex(strLong.getBytes()));
		    	
		    	//sFieldData = fmt.format(sOutMask, sCadena1).toString();
		    	//sFieldData = fmt.format(sOutMask, strLong).toString();
		    	//System.out.printf("............................................................... %s", strLong );
		    
		    	//sFieldData = fmt.format("%07d", sCadena1).toString();
		    	//  ok   sFieldData = fmt.format("%07d", l).toString();
		        sFieldData = fmt.format(sOutMask, l).toString();
		        
		        //System.out.println(" sFieldData: " + sFieldData + " Bytes2Hex:" + BytesToHex(sFieldData.getBytes())); 
		         
		        //System.out.println("\n.............................................................. PD Packed: " + sFieldData + " Bytes2Hex:" + BytesToHex(sFieldData.getBytes()));		         
		    };
		    			    			    
		    if (sType.equals("D")) {  
		        Boolean  bValidAAAAMMDD = IsAAAAMMDD(sFieldData);
		        if (!bValidAAAAMMDD) {  
			    	listError.add(" Input Register: " + lNReg
			    			    + " Not valid AAAAMMDD Date at Field: " + sFieldName);	
			    };
		    };
		    
		    if (sType.equals("N")) {  
		    	Boolean  bNumeric = IsNumeric(sFieldData);
		        if (!bNumeric) {  
			    	listError.add(" Input Register: " + lNReg
			    			+ " Not Numbers at Field: " + sFieldName);	
			    };
		    };
		    		    
		    //System.out.println("Field END: " + sFieldName + " Data: " + sFieldData + " Bytes2Hex:" + BytesToHex(sFieldData.getBytes()));
		    
		    sReg2Write = sReg2Write.concat(sFieldData);
		    //System.out.println("sReg2Write: " + sReg2Write + " Mide: " + sReg2Write.length());
		    
		} //(sTipReg.equals("1"))
					
	}; //for(String sRule : listFormat)		
	
	return sReg2Write;    // Devuelve InReg outmasked 
	
} //ValidateReg
 

private static Boolean IsNumeric(String sData) {
	
	for(int i = 0, n = sData.length() ; i < n ; i++) { 
	    char c = sData.charAt(i); 
	    if (!Character.isDigit(c)) return false;
	};
	
	return true;
} //IsNumeric

 

private static Boolean IsAAAAMMDD(String sData) {
	
	for(int i = 0, n = sData.length() ; i < n ; i++) { 
	    char c = sData.charAt(i); 
	    if (!Character.isDigit(c)) return false;
	};
	
    if(sData.length()!=8) return false;
    	    	    
    int year=Integer.parseInt(sData.substring(0,4));
    int month=Integer.parseInt(sData.substring(4,6));
    int date=Integer.parseInt(sData.substring(6,8));
    
    if(month>12) return false;
    if(date>31) return false;
    
    if (month < 1 || month > 12)  return false;
    
    int days = 31;
    
    if (month == 2) {
        days = 28;
        if ((year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0))) days = 29;
    }
    else {
    	
    	if (month == 4 || month == 6 || month == 9 || month == 11) {
    		days = 30;
        };	        
    };
    
    if (date < 1 || date > days) return false;
     
    return true;
    		
} //IsAAAAMMDD


public static void PrintListError() {
	
	BufferedWriter bw = null;
	FileWriter fw = null;
	
	try {
		fw = new FileWriter(FILENAME+"_errors.txt");
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	};
	bw = new BufferedWriter(fw);

	for(String sErrorLine : listError){

		System.out.println("Error: " + sErrorLine);	 
		try {
			bw.write(sErrorLine);
			bw.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}; 			

	}; //for(String sErrorLine : listError)	
	
	if (bw != null)
		try {
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};

	if (fw != null)
		try {
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		
	System.out.println("PrintListError Ended");
} //PrintListError() Ends


public static void WriteStatFile(Long lRegs) {
	
	BufferedWriter bw = null;
	FileWriter fw = null;
	File f = null;
	 
	
	try {
		fw = new FileWriter(FILENAME+"_stats_" + (getCurrentTimeStamp()) +".txt");
		f = new File(FILENAME);
	} catch (IOException e) {		
		e.printStackTrace();
	};
	
	bw = new BufferedWriter(fw);

	try {
			bw.write("File;"+f.getName()+";Registros;" + lRegs);
			bw.newLine();
		} catch (IOException e) {			
			e.printStackTrace();
	}; 			
	 	
	if (bw != null)
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		};

	if (fw != null)
		try {
			fw.close();
		} catch (IOException e) {			
			e.printStackTrace();
		};
		
	System.out.println("Write Statatistics file Ended: "+ FILENAME+"_stats.txt" + " Regs:"+lRegs );
} //WriteStatFile Ends


public static String getCurrentTimeStamp() {
    return new SimpleDateFormat("yyyy-MM-dd HHmmssSSS").format(new Date());
}


public static String GetHexString (byte[] buf) 
{
     
    StringBuffer sb = new StringBuffer();

    for (byte b:buf)
    {
        //sb.append(String.format("%x", b));
        //sb.append(Integer.toString(b, 16));
        sb.append(Integer.toString(b));
    }

        return sb.toString();
}


public static String BytesToHex(byte[] bIn) {
    final StringBuilder builder = new StringBuilder();
    for(byte b : bIn) {
        builder.append(String.format("%02x", b));
    }
    return builder.toString();
}


public static byte[] CleanX00 (byte[] buf, String sFieldName) 
{
     
    StringBuffer sb = new StringBuffer();
    byte bx00= 0x00;
    boolean bEsx00 = false;  
    byte[] bCleanedBuf = buf.clone();
    int iPosB = 0;

    for (byte b:buf)
    {
    	if (bx00  == b) {
    		//System.out.println(" Hay x00 !! " ); 
    		b = 0x40;      		 
        };	        
        
        if (sFieldName.equals("ALFPRIAPEL")) b = 0x40;
		if (sFieldName.equals("ALFSEGAPEL")) b = 0x40;
		if (sFieldName.equals("ALFNOMBRE"))  b = 0x40;
		
        sb.append(Integer.toString(b));
        bCleanedBuf[iPosB] = b;
        iPosB++;
    }

        return bCleanedBuf;
}


public static String Convert (String strToConvert,String in, String out){
    
	try {
        Charset charset_in = Charset.forName(out);
        Charset charset_out = Charset.forName(in);

        CharsetDecoder decoder = charset_out.newDecoder();
        CharsetEncoder encoder = charset_in.newEncoder();

        CharBuffer uCharBuffer = CharBuffer.wrap(strToConvert);

        ByteBuffer bbuf = encoder.encode(uCharBuffer);

        CharBuffer cbuf = decoder.decode(bbuf);

        String s = cbuf.toString();

        //System.out.println("Original String is: " + s);
        return s;

    } catch (CharacterCodingException e) {
        //System.out.println("Character Coding Error: " + e.getMessage());
        return "";
    }
  }


/**
 *  https://www.ibm.com/support/knowledgecenter/SSQ2R2_14.0.0/com.ibm.etools.cbl.win.doc/topics/cpari09.htm (COMP-3)
 **/	   
private static String unpackPD(byte[] packedData, int decimals) {
	  
	  String unpackedData="";
	  final int negativeSign = 13;
	  int lengthPack = packedData.length;
	  int numDigits = lengthPack*2-1;

	  int raw = (packedData[lengthPack-1] & 0xFF);
	  int firstDigit = (raw >> 4);
	  int secondDigit = (packedData[lengthPack-1] & 0x0F);
	  boolean negative = (secondDigit==negativeSign);
	  int lastDigit = firstDigit;
	  for (int i = 0; i < lengthPack-1; i++) {
		  raw = (packedData[i] & 0xFF);
		  firstDigit = (raw >> 4);
		  secondDigit = (packedData[i] & 0x0F);
		  unpackedData+=String.valueOf(firstDigit);
		  unpackedData+=String.valueOf(secondDigit);

	  }
	  unpackedData+=String.valueOf(lastDigit);
	  if (decimals > 0) {
		  unpackedData = unpackedData.substring(0,numDigits-decimals)+"."+unpackedData.substring(numDigits-decimals);
	  }
	  if (negative){
		  return '-'+unpackedData;
	  }
	  return unpackedData;
}  // unpackPD


}                                                                                           //public class XpandEBCDIC
