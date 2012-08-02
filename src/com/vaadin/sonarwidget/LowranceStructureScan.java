package com.vaadin.sonarwidget;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * No public spec exists for Lowrance sonar format.
 * This thread has been for great help in reverse 
 * engineering the file format. 
 * http://www.geotech1.com/forums/showthread.php?t=11159
 * @author samuli
 *
 */
public class LowranceStructureScan {
	private int format;
	private File file;
	
	public LowranceStructureScan(File file) throws IOException {
		this.file = file;
		
		DataInputStream datainput = new DataInputStream(new FileInputStream(file));
		format = Integer.reverseBytes(datainput.readInt());		
		Short.reverseBytes(datainput.readShort());
		datainput.close();
	}
	
	public int getFormat() {
		return this.format;
	}
	
	private static int HEADER_SIZE = 40;
	
	public Ping[] getPingRange(int offset, int channel, int length) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		long ptr = 8;
		int bytes = 0; 
		Ping[] retval = new Ping[length];

		raf.seek(ptr);
		
		for(int skip=0; skip < offset*3; skip++) {
			int blocklen = getBlockHeader(raf).length;
			bytes += blocklen-HEADER_SIZE;
			ptr += blocklen+HEADER_SIZE;
			raf.seek(ptr);
		}
		
		for(int loop=0; loop < length; loop++) {
			try {
				Header header = null;
				Ping ping = null;
				while(true) {
					header = getBlockHeader(raf);
					bytes += header.length+HEADER_SIZE;
					
					if(header.type.ordinal() == channel) {
						ping = new Ping(raf, header.length);
						ptr += header.length+HEADER_SIZE;
						break;
					}
					ptr += header.length+HEADER_SIZE;
					raf.seek(ptr);
				}
				
				retval[loop] = ping;
				
			} catch(Exception e) {
				System.out.println("Exception at "+bytes+", "+e.getMessage());
				e.printStackTrace();
			}
		}
		
		raf.close();
		return retval;
	}

	private Header getBlockHeader(RandomAccessFile inputstream) throws IOException {
		Header header = new Header();
		byte[] headerbytes = readBytes(inputstream, HEADER_SIZE);
		int lenbits = toBigEndianInt(headerbytes, 32);
		if(lenbits == 0x05A00002) {
			header.length = 1584;
			header.type = Type.eDownScan;
		} else if(lenbits == 0x0B400005) {
			header.length = 3024;
			header.type = Type.eSideScan;
		} else if(lenbits == 0x0C000000) {
			header.length = 3216;
			header.type = Type.eTraditional;
		} 
		
		if(header.length == 0) {
			throw new IllegalStateException(
				String.format(
					"len cannot be determined from: %04x", 
					lenbits								
				)
			);
		}
		
		header.length -= HEADER_SIZE;
		return header;
	}
	
	private byte[] readBytes(RandomAccessFile stream, int len) throws IOException {
		byte[] bytes = new byte[len];
		stream.read(bytes, 0, len);
		return bytes;
	}
	
	private int toBigEndianInt(byte[] raw, int offset) {
		return 0xFF000000&(raw[offset+3]<<24) | 0x00FF0000&(raw[offset+2]<<16) | 0x0000FF00&(raw[offset+1]<<8) | 0x000000FF&raw[offset];
	}
	
	private float toBigEndianFloat(byte[] raw, int offset) {
		return Float.intBitsToFloat(toBigEndianInt(raw, offset));
	}
	
	public enum Type {eTraditional, eDownScan, eSideScan}
	
	public class Header {
		int length = 0;
		Type type;
	}
	
	public class Ping {
		private float lowLimit;
		private float depth;
		private float temp;
		
		private int positionX;
		private int positionY;
		private int timeOffset;
		private float speed;
		private float track;
		private Type type;
		
		
		private byte[] soundings;
		
		private String debug;
		
		private static final double RAD_CONVERSION = 180/Math.PI;
		private static final double EARTH_RADIUS = 6356752.3142;
		private static final float FEET_TO_METERS = 0.3048f;
		private static final float KNOTS = 1.852f;

		
		public Ping(RandomAccessFile inputstream, int len) throws IOException {
			int pingheader = 104;
			byte[] header = readBytes(inputstream, pingheader);
			debug = "";
			
//			for(int loop=0; loop < pingheader; loop+=4) {
//				int valuei = toBigEndianInt(header, loop);
//				float valuef = toBigEndianFloat(header, loop);			
//				debug += String.format("off: "+loop+" value: %02x, %d, %f\n", valuei, valuei, valuef);
//			}
			
			lowLimit = toBigEndianFloat(header, 4);
			timeOffset = toBigEndianInt(header, 100);
			depth = toBigEndianFloat(header, 24);
			speed = toBigEndianFloat(header, 60);
			temp = toBigEndianFloat(header, 64);
			positionX = toBigEndianInt(header, 68);
			positionY = toBigEndianInt(header, 72);
			track = toBigEndianFloat(header, 80);

			soundings = readBytes(inputstream, len-pingheader);
		}

		
		public float getDepth() {
			return this.depth*FEET_TO_METERS;
		}
		
		public float getTemp() {
			return this.temp;
		}
		
		public int getTimeStamp() {
			return this.timeOffset;
		}
		
		public float getSpeed() {
			return this.speed*KNOTS;
		}
		
		public byte[] getSoundings() {
			return this.soundings;
		}
		
		public float getLowLimit() {
			return this.lowLimit*FEET_TO_METERS;
		}
		
		public float getTrack() {
			return this.track;
		}

		
		/**
		 * Convert Lowrance mercator meter format into WGS84.
		 * Used this article as a reference: http://www.oziexplorer3.com/eng/eagle.html
		 * @return
		 */
		public double getLongitude() {
			return this.positionX/EARTH_RADIUS * RAD_CONVERSION;
		}
		
		public double getLatitude() {
			double temp = this.positionY/EARTH_RADIUS;
			temp = Math.exp(temp);
			temp = (2*Math.atan(temp))-(Math.PI/2);
			return temp * RAD_CONVERSION;			
		}
		
		public Type getType() {
			return this.type;
		}
		
		@Override
		public String toString() {
			return debug+"\n"+
					", lowlimit: "+getLowLimit()+
					", depth: "+getDepth()+
					", temp: "+getTemp()+					
					", pos: "+Double.toString(getLatitude())+"/"+Double.toString(getLongitude())+
					", time: "+getTimeStamp()+					
					", speed: "+getSpeed()+
					", track: "+getTrack()+
					"";
		}
	}

}
