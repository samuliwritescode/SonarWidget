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
		
		RandomAccessFile inputstream = new RandomAccessFile(file, "r");
		inputstream.seek(10+blocksize*index);
		
		Ping[] retval = new Ping[length];
		for(int loop=0; loop < length; loop++) {
			//inputstream.seek(10+blocksize*(index+loop));
			retval[loop] = new Ping(inputstream);
		}
		
		return retval;
	}
	
	public Ping getPing(int index) throws IOException {
		RandomAccessFile inputstream = new RandomAccessFile(file, "r");
		inputstream.seek(10+blocksize*index);
		return new Ping(inputstream);
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

		
		public Ping(RandomAccessFile inputstream) throws IOException {
			long offsetBefore = inputstream.getFilePointer();
			mask = toBigEndianShort(inputstream.readShort());
					
			lowLimit = isPresent(LOWERLIMIT)?toBigEndianFloat(inputstream.readInt()):0;
			depth = isPresent(DEPTH)?toBigEndianFloat(inputstream.readInt()):0;
			temp = isPresent(WATERTEMP)?toBigEndianFloat(inputstream.readInt()):0;
			//waterSpeed = isPresent(WATERSPEED)?toBigEndianFloat(inputstream.readInt()):0;
			
			positionY = isPresent(POSITION)?toBigEndianInt(inputstream.readInt()):0;
			positionX = isPresent(POSITION)?toBigEndianInt(inputstream.readInt()):0;
			
			surfaceDepth = isPresent(SURFACEDEPTH)?toBigEndianFloat(inputstream.readInt()):0;
			topOfBottomDepth = isPresent(TOPOFBOTTOMDEPTH)?toBigEndianFloat(inputstream.readInt()):0;
			timeOffset = isPresent(TIMEOFFSET)?toBigEndianInt(inputstream.readInt()):0;
			
			speed = isPresent(SPEED)?toBigEndianFloat(inputstream.readInt()):0;
			track = isPresent(TRACK)?toBigEndianFloat(inputstream.readInt()):0;
			altitude = isPresent(ALTITUDE)?toBigEndianFloat(inputstream.readInt()):0;
			
			rate = toBigEndianInt(inputstream.readInt());
			
			long headerlen = inputstream.getFilePointer()-offsetBefore;
			soundings = new byte[(int) (blocksize-headerlen)];
			for(int loop=0; loop < blocksize-headerlen; loop++) {
				soundings[loop] = inputstream.readByte();				
			}
			
		}
		
		private int toBigEndianInt(int littleendian) {
			return Integer.reverseBytes(littleendian);
		}
		
		private float toBigEndianFloat(int littleendian) {
			return Float.intBitsToFloat(toBigEndianInt(littleendian));
		}
		
		private short toBigEndianShort(short littleendian) {
			return Short.reverseBytes(littleendian);
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
