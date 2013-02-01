package org.vaadin.sonarwidget.data;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * SL2 file format is not documented publicly
 * and implementation here is obtained by
 * analyzing a binary file.
 * @author samuli
 *
 */
public class LowranceStructureScan extends AbstractLowrance {
	private File file;
	private Type type;
	private static int HEADER_SIZE = 40;
	
	private List<Long> pointerTable = new ArrayList<Long>();
	
	public LowranceStructureScan(File file, Type channel) throws IOException {
		this.file = file;
		this.type = channel;
		
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		try {
			long ptr = 8;
			raf.seek(ptr);
			
			while(true) {
				Header blockHeader = getBlockHeader(raf);
				if(blockHeader.type == channel) {
					pointerTable.add(ptr);
				}
	
				ptr += blockHeader.length+HEADER_SIZE;
				if(ptr >= raf.length()) {
					break;
				}
	
				raf.seek(ptr);
			}
		} finally {
			raf.close();			
		}
	}
	
	@Override
	public Type getType() {
		return this.type;
	}

	@Override
	public long getLength() {
		return this.pointerTable.size();
	}
	
	@Override
	public Ping[] getPingRange(int offset, int length) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		try {
			StructurePing[] retval = new StructurePing[length];
			
			if(offset+length > pointerTable.size()) {
				throw new IndexOutOfBoundsException("offset+length beyond file length");
			}
			
			for(int loop=0; loop < length; loop++) {
				raf.seek(pointerTable.get(offset+loop));
				Header header = getBlockHeader(raf);
				retval[loop] = new StructurePing(raf, header.length);
			}
			
			return retval;
		} finally {
			raf.close();			
		}
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

	private class Header {
		int length = 0;
		Type type;
	}
	
	private class StructurePing implements Ping{
		private float lowLimit;
		private float depth;
		private float temp;
		
		private int positionX;
		private int positionY;
		private int timeOffset;
		private float speed;
		private float track;
	
		private byte[] soundings;
		
		public StructurePing(RandomAccessFile inputstream, int len) throws IOException {
			int pingheader = 104;
			byte[] header = readBytes(inputstream, pingheader);
			
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
		
		/**
		 * Convert Lowrance mercator meter format into WGS84.
		 * Used this article as a reference: http://www.oziexplorer3.com/eng/eagle.html
		 * @return
		 */
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
