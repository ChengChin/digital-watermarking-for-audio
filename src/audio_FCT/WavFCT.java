/*
reference:
http://blog.xuite.net/huntertsai1211/comic/9581128-MATLAB%E4%B8%AD%E7%9A%84%E9%9B%A2%E6%95%A3%E5%82%85%E7%AB%8B%E8%91%89%E8%BD%89%E6%8F%9B+
http://www.labbookpages.co.uk/audio/javaWavFiles.html
*/
package audio_FCT;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.math3.transform.*;
import java.io.FileWriter;



public class WavFCT
{
	private static WavFile wavFile;
	private static int duplication = 10;			//for message
	private static int messageLength = 4;			//for extract
	private static int SEGMENT = 16385; 			//do FCT every segment
	private static double boundary = 0;				//extracting judgement 1 or 0
	private static double up_bd = 0.01;				//embeded for bit 1
	private static double down_bd = 0.001;			//embeded for bit 0
	
	private static int[] less;
	
	public WavFCT(File cover)						//read file
	{
		try
		{
			wavFile = WavFile.openWavFile(cover);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	public static double[] FCT(double[] cover)     //transform by part
	{
		FastCosineTransformer fct = new FastCosineTransformer(DctNormalization.ORTHOGONAL_DCT_I); //ORTHOGONAL_DCT_I STANDARD_DCT_I 
		
		int NUMOFPART = (cover.length/SEGMENT + 1);
		double[] rst = new double[cover.length];   //result to return
		
		//lessAmpIndex
		less = new int[cover.length / SEGMENT];
		
		//trasform
		double[] temp_org = new double[SEGMENT];
		double[] temp_trs = new double[SEGMENT];
		int index = 0;
		for(int i = 0; i < (NUMOFPART - 1); i++)
		{
			System.arraycopy(cover,(i * SEGMENT),temp_org,0,SEGMENT);
			temp_trs = fct.transform(temp_org,TransformType.FORWARD);
			
			//test less
			less[i] = lessAmpIndex(temp_trs);
			
			System.arraycopy(temp_trs,0,rst,(i * SEGMENT),SEGMENT);
		}
		//copy last part(short than SEGMENT)
		System.arraycopy(cover,((NUMOFPART - 1)*SEGMENT),rst,((NUMOFPART - 1)*SEGMENT),(cover.length % SEGMENT)); 
		
		return rst;
	}
	public static double[] iFCT(double[] cover)
	{
		FastCosineTransformer fct = new FastCosineTransformer(DctNormalization.ORTHOGONAL_DCT_I);
	
		int NUMOFPART = (cover.length/SEGMENT + 1);
		double[] rst = new double[cover.length];   //result to return
		
		//trasform
		double[] temp_org = new double[SEGMENT];
		double[] temp_trs = new double[SEGMENT];
		int index = 0;
		for(int i = 0; i < (NUMOFPART - 1); i++)
		{
			System.arraycopy(cover,(i * SEGMENT),temp_org,0,SEGMENT);
			temp_trs = fct.transform(temp_org,TransformType.INVERSE);
			System.arraycopy(temp_trs,0,rst,(i * SEGMENT),SEGMENT);
		}
		//copy last part(short than SEGMENT)
		System.arraycopy(cover,((NUMOFPART - 1)*SEGMENT),rst,((NUMOFPART - 1)*SEGMENT),(cover.length % SEGMENT)); 
		
		return rst;
	}
	public static byte[] dupicate(byte[] message) 
	{
		byte[] msg = new byte[message.length*duplication];
		for(int i = 0; i < duplication; i++)
		{
			System.arraycopy(message,0,msg,(i*message.length),message.length);
		}
		return msg;
	}
	public static void embedMessage(double[] cover,byte[] message)
	{
		int index = 0;
		int x = 0;
		byte[] dupMessage = dupicate(message);
		for(int i = 0; i < dupMessage.length; i++)
		{
			byte msg = dupMessage[i];
			for(int b = 7; b >= 0;b--,index+=SEGMENT)  
			{
				index = x*SEGMENT;
				index += less[x];
				int bit = ((msg>>b)&1);
				if(bit==1)
				{
					if(cover[index]>0)
						cover[index] = up_bd;
					else
						cover[index] = (-1)*up_bd;
				}	
				else
				{
					if(cover[index]>0)
						cover[index] = down_bd;
					else
						cover[index] = (-1)*down_bd;
				}
				x++;
			}
		}
	}
	public static byte[] extractMessage(double[] cover)
	{
		int index = 0;
		int x = 0;
		byte[] message = new byte[messageLength*duplication];
		for(int i = 0;i < message.length;i++)
		{
			for(int b = 0;b < 8;b++,index+=SEGMENT)
			{
				index = x*SEGMENT;
				index += less[x];
				//System.out.println(less[x]);
				if(Math.abs(cover[index]) >boundary)
					message[i] = (byte)((message[i]<<1) | 1);
				else
					message[i] = (byte)((message[i]<<1) | 0);
				x++;
			}
		}
		return message;
	}
	public static double compare(byte[] test1,byte[] test2)
	{
		float count = 0;
		for(int i = 0 ;i < test1.length; i++)
		{
			for(int b = 7;b >= 0; b--)
			{
				if((int)((int)test1[i]>>b & 1) == (int)((int)test2[i]>>b & 1))
					count++;
			}
		}
		return (count/(test1.length*8)*100);
	}
	private static int lessAmpIndex(double[] cover)
	{
		int min_index = 0;
		double x = Math.abs(cover[0]);
		for(int i = 1;i < (cover.length - 1);i++)
		{
			if(x > Math.abs(cover[i]))
			{
				min_index = i;
				x = Math.abs(cover[i]);	
			}
		}
		return min_index;
	}
	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	/*public static void main(String[]args)
	{
		byte[] test1 = new byte[0];		//for message compare(original message)
		byte[] test2 = new byte[0];		//for message compare(extracted message) 
		
		//	EmbedMessage
		//-----------------------------------------------------------------------------------
		try
		{
			System.out.println("Start Embed....");
			File f = new File("test/1.wav");			//original cover
			WavFCT w = new WavFCT(f);
			
			// return the number of wav information
                                                      long  numFrames = wavFile.getNumFrames();
			long  sampleRate = wavFile.getSampleRate();
			int     validBits = wavFile.getValidBits();
			int     numChannels = wavFile.getNumChannels();
			
			//show the WAV information
			System.out.printf("\n");
			System.out.println("WAV information: ");
			System.out.println("Sample rate:"+wavFile.getSampleRate()+" Channels:"+wavFile.getNumChannels()+" Frames:"+wavFile.getNumFrames());
			System.out.println("Valid bits:"+wavFile.getValidBits());
			System.out.printf("\n");
			
			//get music data
			double[] coverDouble = new double[(int)numFrames*numChannels];
			
			wavFile.readFrames(coverDouble,coverDouble.length);
			
			//FCT transform
			double[] fct_trs = FCT(coverDouble);
			
			//read message
			File msg = new File("test/message.txt");
			byte[] message = Files.readAllBytes(msg.toPath());
			
			test1 = dupicate(message);
			
			//embedMessage
			embedMessage(fct_trs,message);
		
			//iFCT transform
			double[] ifct_trs = iFCT(fct_trs);
			
			// Determine how many frames to write
			int toWrite = ifct_trs.length/2;
			
			//create wav file
			WavFile writeWavFile = wavFile.newWavFile(new File("out.wav"), 2, toWrite, 16, 44100);
			writeWavFile.writeFrames(ifct_trs,toWrite);		//( Arrays , Number of Frames)

			writeWavFile.close();
			System.out.println("Finish embeded!");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		//	Extract Message
		//--------------------------------------------------------------------------------------------------
		try
		{
			System.out.println("Start Extract....");
			File f2 = new File("./out.wav");			//embeded cover
			WavFCT w2 = new WavFCT(f2);
			
			// return the number of wav information
                                                      long numFrames = wavFile.getNumFrames();
			long sampleRate = wavFile.getSampleRate();
			int validBits = wavFile.getValidBits();
			int numChannels = wavFile.getNumChannels();
			
			//show the WAV information
			System.out.printf("\n");
			System.out.println("WAV information: ");
			System.out.println("Sample rate:"+wavFile.getSampleRate()+" Channels:"+wavFile.getNumChannels()+" Frames:"+wavFile.getNumFrames());
			System.out.println("Valid bits:"+wavFile.getValidBits());
			System.out.printf("\n");
			
			//get music data
			double[] coverDouble = new double[(int)numFrames*numChannels];
			wavFile.readFrames(coverDouble,coverDouble.length);
			
			//FCT transform
			double[] fct_trs = FCT(coverDouble);
			
			//Get Message
			FileWriter fw = new FileWriter("./test.txt");
			
			//analysis
			while(boundary < Math.abs(up_bd))
			{
				byte[] message = extractMessage(fct_trs);
				test2 = message;					
				fw.write( boundary + "\t:\t" + compare(test1,test2) + "\r\n");
				fw.flush();
				//compare(test1,test2);
				boundary += 0.00001;
			}
			fw.close();
			
			//create message
			boundary = 0.001;
			byte[] message = extractMessage(fct_trs);
			//Create Message
			File saveExtractedMessage = new File("./message.txt");
			Files.write(saveExtractedMessage.toPath(),message);

			
			System.out.println("Finish Extract!");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}*/

}