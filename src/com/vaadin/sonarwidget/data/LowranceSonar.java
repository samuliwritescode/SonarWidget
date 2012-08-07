package com.vaadin.sonarwidget.data;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * No public spec exists for Lowrance sonar format.
 * This thread has been for great help in reverse 
 * engineering the file format. 
 * http://www.geotech1.com/forums/showthread.php?t=11159
 * @author samuli
 *
 */
public class LowranceSonar extends AbstractLowrance {
	private int format;
	private int blocksize;
	private File file;
	private long blocks;
	
	public LowranceSonar(File file) throws IOException {
		this.file = file;
		
		DataInputStream datainput = new DataInputStream(new FileInputStream(file));
		try {
			format = Integer.reverseBytes(datainput.readInt());		
			blocksize = Integer.reverseBytes(datainput.readInt());
			Short.reverseBytes(datainput.readShort());
			blocks = (file.length()-10) / blocksize;
		} finally {
			datainput.close();
		}
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
	
	@Override
	public Type getType() {
		return Type.eTraditional;
	}
	
	public Ping[] getPingRange(int index, int length) throws IOException {
		
		DataInputStream inputstream = new DataInputStream(new FileInputStream(file));
		
		try {
			inputstream.skip(10+blocksize*index);
			
			Ping[] retval = new LowrancePing[length];
			
			for(int loop=0; loop < length; loop++) {
				retval[loop] = new LowrancePing(inputstream);
			}
			
			return retval;
		} finally {
			inputstream.close();
		}
	}
	
	private class LowrancePing implements Ping {
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

		public LowrancePing(DataInputStream inputstream) throws IOException {
			long headerlen = 2;
			mask = toBigEndianShort(inputstream.readShort());
			
			if(mask == 27924) {
				headerlen += 48;
				byte[] headers =readBytes(inputstream, 48);
				
				lowLimit = toBigEndianFloat(headers, 0);
				depth = toBigEndianFloat(headers, 4);
				temp = toBigEndianFloat(headers, 8);
			
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
			else if (mask == 27925) {
				headerlen += 52;
				
				byte[] headers = readBytes(inputstream, 52);
				
				lowLimit = toBigEndianFloat(headers, 0);
				depth = toBigEndianFloat(headers, 4);
				temp = toBigEndianFloat(headers, 8);
				positionY = toBigEndianInt(headers, 12);
				positionX = toBigEndianInt(headers, 16);
				
				surfaceDepth = toBigEndianFloat(headers, 20);
				topOfBottomDepth = toBigEndianFloat(headers, 24);
				toBigEndianFloat(headers, 28);
				
				timeOffset = toBigEndianInt(headers, 32);
				speed = toBigEndianFloat(headers, 36);
				track = toBigEndianFloat(headers, 40);
				altitude = toBigEndianFloat(headers, 44);				
				rate = toBigEndianInt(headers, 48);
			}
			else if(mask == 27926) {
				headerlen += 56;
				byte[] headers = readBytes(inputstream, 56);

				lowLimit = toBigEndianFloat(headers, 0);
				depth = toBigEndianFloat(headers, 4);
				temp = toBigEndianFloat(headers, 8);
				positionY = toBigEndianInt(headers, 12);
				positionX = toBigEndianInt(headers, 16);
				
				surfaceDepth = toBigEndianFloat(headers, 20);
				topOfBottomDepth = toBigEndianFloat(headers, 24);
				toBigEndianFloat(headers, 28);
				toBigEndianFloat(headers, 32);
				timeOffset = toBigEndianInt(headers, 36);
				speed = toBigEndianFloat(headers, 40);
				track = toBigEndianFloat(headers, 44);
				altitude = toBigEndianFloat(headers, 48);
				
				rate = toBigEndianInt(headers, 52);
			}
			else if(mask == 28436) {
				headerlen += 48;
				
				byte[] headers = readBytes(inputstream, 48);
				
				lowLimit = toBigEndianFloat(headers, 0);
				depth = toBigEndianFloat(headers, 4);
				temp = toBigEndianFloat(headers, 8);
				positionY = toBigEndianInt(headers, 12);
				positionX = toBigEndianInt(headers, 16);
				surfaceDepth = toBigEndianFloat(headers, 20);
				topOfBottomDepth = toBigEndianFloat(headers, 24);
				
				timeOffset = toBigEndianInt(headers, 28);
				
				speed = toBigEndianFloat(headers, 32);
				track = toBigEndianFloat(headers, 36);
				altitude = toBigEndianFloat(headers, 40);
				
				rate = toBigEndianInt(headers, 44);
				
			} else {
				System.out.println("unknown mask: "+mask);
			}
						
			soundings = new byte[(int) (blocksize-headerlen)];
			inputstream.read(soundings, 0, (int) (blocksize-headerlen));			
		}
		
		@Override
		public float getDepth() {
			return toMetres(this.depth);
		}
		
		@Override
		public float getTemp() {
			return this.temp;
		}
		
		@Override
		public int getTimeStamp() {
			return this.timeOffset;
		}
		
		@Override
		public float getSpeed() {
			return toKilometersPerHour(this.speed);
		}
		
		@Override
		public byte[] getSoundings() {
			return this.soundings;
		}
		
		@Override
		public float getLowLimit() {
			return toMetres(this.lowLimit);
		}
		
		@Override
		public float getTrack() {
			return this.track;
		}

		@Override
		public double getLongitude() {
			return toLongitude(this.positionX);
		}
		
		@Override
		public double getLatitude() {
			return toLatitude(this.positionY);		
		}
	}
}
