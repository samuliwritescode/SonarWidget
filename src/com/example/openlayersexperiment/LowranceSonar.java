package com.example.openlayersexperiment;

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
public class LowranceSonar {
	private int format;
	private int blocksize;
	private File file;
	private long blocks;
	
	
	public LowranceSonar(File file) throws IOException {
		this.file = file;
		
		DataInputStream datainput = new DataInputStream(new FileInputStream(file));
		format = Integer.reverseBytes(datainput.readInt());		
		blocksize = Integer.reverseBytes(datainput.readInt());
		short wat = Short.reverseBytes(datainput.readShort());
		blocks = (file.length()-10) / blocksize;
		datainput.close();
	}
	
	public int getFormat() {
		return this.format;
	}
	
	public int getBlockSize() {
		return this.blocksize;
	}
	
	public long getLength() {
		return this.blocks;
	}
	
	public Ping[] getPingRange(int index, int length) throws IOException {
		
		DataInputStream inputstream = new DataInputStream(new FileInputStream(file));
		inputstream.skip(10+blocksize*index);
		
		Ping[] retval = new Ping[length];
		
		for(int loop=0; loop < length; loop++) {
			retval[loop] = new Ping(inputstream);
		}
		
		return retval;
	}
	
	public class Ping {
		private short mask;
		private float lowLimit;
		private float depth;
		private float temp;
		private float waterSpeed;
		
		private int positionX;
		private int positionY;
		private float surfaceDepth;
		private float topOfBottomDepth;
		private int timeOffset;
		private float speed;
		private float track;
		private float altitude;
		private int rate;
		
		private byte[] soundings;
		
		private static final short WATERTEMP = 16 | 128;
		private static final short WATERSPEED = 16 | 128;

		private static final short LOWERLIMIT = 1024 | 2048 | 8192;
		private static final short DEPTH = 1024 | 2048 | 8192;
		private static final short SURFACEDEPTH = 1024 | 2048 | 8192;
		private static final short TOPOFBOTTOMDEPTH = 1024 | 2048 | 8192;
		private static final short TIMEOFFSET = 1024 | 2048 | 8192;

		private static final short POSITION = 4 | 256 | 16384;
		private static final short SPEED = 4 | 256 | 16384;
		private static final short TRACK = 4 | 256 | 16384;
		private static final short ALTITUDE = 4 | 256 | 16384;
		
		private static final double RAD_CONVERSION = 180/Math.PI;
		private static final double EARTH_RADIUS = 6356752.3142;
		private static final float FEET_TO_METERS = 0.3048f;
		private static final float KNOTS = 1.852f;

		
		public Ping(DataInputStream inputstream) throws IOException {
			long headerlen = 2;
			mask = toBigEndianShort(inputstream.readShort());
			
			if(mask == 0x6D14) {
				headerlen += 48;
				byte[] headers = new byte[48];
				inputstream.read(headers, 0, 48);
				
				lowLimit = toBigEndianFloat(headers, 0);
				depth = toBigEndianFloat(headers, 4);
				temp = toBigEndianFloat(headers, 8);
				//waterSpeed = isPresent(WATERSPEED)?toBigEndianFloat(inputstream.readInt()):0;
				
				positionY = toBigEndianInt(headers, 12);
				positionX = toBigEndianInt(headers, 16);
				
				surfaceDepth = toBigEndianFloat(headers, 20);
				topOfBottomDepth = toBigEndianFloat(headers, 24);
				timeOffset = toBigEndianInt(headers, 28);
				
				speed = toBigEndianFloat(headers, 32);
				track = toBigEndianFloat(headers, 36);
				altitude = toBigEndianFloat(headers, 40);
				
				rate = toBigEndianInt(headers, 44);
				
			}
			
			soundings = new byte[(int) (blocksize-headerlen)];
			inputstream.read(soundings, 0, (int) (blocksize-headerlen));			
		}
		
		private int toBigEndianInt(byte[] raw, int offset) {
			return 0xFF000000&(raw[offset+3]<<24) | 0x00FF0000&(raw[offset+2]<<16) | 0x0000FF00&(raw[offset+1]<<8) | 0x000000FF&raw[offset];
			//return Integer.reverseBytes(littleendian);
		}
		
		private float toBigEndianFloat(byte[] raw, int offset) {
			return Float.intBitsToFloat(toBigEndianInt(raw, offset));
		}
		
		private short toBigEndianShort(short littleendian) {
			
			return (short) (((0xFF00&littleendian)>>8)&0x00FF |
					((0x00FF&littleendian)<<8)&0xFF00);
			//return Short.reverseBytes(littleendian);
		}
		
		private boolean isPresent(short field) {
			return (field & mask) != 0;
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
		
		@Override
		public String toString() {
			return Short.toString(mask)+
					", lowlimit: "+Float.toString(this.lowLimit)+
					", depth: "+Float.toString(this.depth)+
					", temp: "+Float.toString(this.temp)+					
					", wspeed: "+Float.toString(this.waterSpeed)+
					", pos: "+Double.toString(getLatitude())+"/"+Double.toString(getLongitude())+
					", surfacedepth: "+Float.toString(this.surfaceDepth)+
					", topofbottomdepth: "+Float.toString(this.topOfBottomDepth)+
					", time: "+Integer.toString(this.timeOffset)+					
					", speed: "+Float.toString(this.speed)+
					", track: "+Float.toString(this.track)+
					", alt: "+Float.toString(this.altitude)+
					", rate: "+Integer.toString(this.rate);
		}
	}

}
