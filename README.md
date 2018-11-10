# XpandEBCDIC
Convert EBCDIC to ASCII, unpacking COMP fields 
This class converts files with IBM EBCDIC format (charset CP1047) to ASCI format (charset ISO-8859-1)
It takes as input argument the format file in plain text, structured as indicated below.  

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
 *  http://www.3480-3590-data-conversion.com/article-signed-fields.html
 *
 */
