package com.vaadin.sonarwidget.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Humminbird DAT/SON reader. On side scan
 * mode will combine right and left channel
 * into one channel.
 * 
 * Spec for format obtained from this thread
 * http://www.xumba.scholleco.com/viewtopic.php?t=118&postdays=0&postorder=asc&start=0
 * @author samuli
 *
 */
public class HumminbirdSSI implements Sonar {
	private static final double RAD_CONVERSION = 180/Math.PI;
	private static final double EARTH_RADIUS = 6356752.3142;
	
	private Type type;
	private File datafile = null;
	private File seconddatafile = null;
	private int blocksize = 0;
	private List<Integer> index = null;
	private List<Integer> secondindex = null;
	
	public HumminbirdSSI(File file, Type channel) throws IOException {
		this.type = channel;
		
		String name = initFromDAT(file);
		String dirname = name.substring(0, name.length()-4);
		String path = file.getParent()!=null?file.getParent()+"/"+dirname:dirname;
		
		switch(channel) { 
		case eTraditional:
			index = getIDXData(new File(String.format("%s/B000.idx", path)));
			datafile = new File(String.format("%s/B000.SON", path));
			break;
		case eDownScan: 
			index = getIDXData(new File(String.format("%s/B001.idx", path)));
			datafile = new File(String.format("%s/B001.SON", path));
			break;
		case eSideScan:
			index = getIDXData(new File(String.format("%s/B002.idx", path)));
			datafile = new File(String.format("%s/B002.SON", path));
			secondindex = getIDXData(new File(String.format("%s/B003.idx", path)));
			seconddatafile = new File(String.format("%s/B003.SON", path));
			break;
		}
	}

	private List<Integer> getIDXData(File idxfile) throws FileNotFoundException,
			IOException {
		RandomAccessFile raf = new RandomAccessFile(idxfile, "r");
		List<Integer> index = new ArrayList<Integer>();
				
		for(int loop=0; loop < raf.length()/8; loop++) {
			int time = raf.readInt();
			int offset = raf.readInt();
			index.add(offset);
		}
		
		return index;
	}

	private String initFromDAT(File file) throws FileNotFoundException, IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		raf.skipBytes(20);
		int timestamp = raf.readInt();
		int longitude = raf.readInt();
		int latitude = raf.readInt();
		
		byte[] namebytes = new byte[10];
		raf.read(namebytes, 0, 10);
		String filename = new String(namebytes);
		raf.skipBytes(2); //skip null character \0000
		
		int ks = raf.readInt();
		int tk = raf.readInt();
		blocksize = raf.readInt();
		raf.close();
		return filename;
	}

	@Override
	public long getLength() {
		return index.size();
	}

	@Override
	public Ping[] getPingRange(int offset, int length) throws IOException {
		if(getType() == Type.eSideScan) {
			//With side images soundings needs to be combined into one array.
			//Assumption is that both channels have same amount of samples. 
			HumminbirdPing[] firstChannel = getPingRangeFromFile(offset, length, datafile, index);
			HumminbirdPing[] secondChannel = getPingRangeFromFile(offset, length, seconddatafile, secondindex);
			
			for(int loop=0; loop < firstChannel.length; loop++) {
				HumminbirdPing first = firstChannel[loop];
				HumminbirdPing second = secondChannel[loop];
				
				byte[] firstsoundings = first.getSoundings();
				byte[] secondsoundings = second.getSoundings();
				reverseBytes(firstsoundings);
				
				byte[] soundings = new byte[firstsoundings.length + secondsoundings.length];
				System.arraycopy(firstsoundings, 0, soundings, 0, firstsoundings.length);
				System.arraycopy(secondsoundings, 0, soundings, firstsoundings.length, secondsoundings.length);
				first.setSoundings(soundings);
			}
			
			return firstChannel;
		} else {
			return getPingRangeFromFile(offset, length, datafile, index);			
		}
	}
	
	private void reverseBytes(byte[] array) {
		int lastindex = array.length-1;
		for(int loop=0; loop < array.length/2; loop++) {
			byte temp = array[loop];
			array[loop] = array[lastindex-loop];
			array[lastindex-loop] = temp;
		}
	}
	
	private HumminbirdPing[] getPingRangeFromFile(int offset, int length, File file, List<Integer> index) throws IOException {
		HumminbirdPing[] pings = new HumminbirdPing[length];
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		for(int loop=0; loop < length; loop++) {
			raf.seek(index.get(offset+loop));
			HumminbirdPing ping = new HumminbirdPing(raf, blocksize);
			pings[loop] = ping;
		}
		return pings;
	}

	@Override
	public Type getType() {
		return this.type;
	}
	
	/**
	 * Convert Lowrance mercator meter format into WGS84.
	 * Used this article as a reference: http://www.oziexplorer3.com/eng/eagle.html
	 * @return
	 */
	protected double toLongitude(int mercator) {
		return mercator/EARTH_RADIUS * RAD_CONVERSION;
	}
	
	protected double toLatitude(int mercator) {
		double temp = mercator/EARTH_RADIUS;
		temp = Math.exp(temp);
		temp = (2*Math.atan(temp))-(Math.PI/2);
		return temp * RAD_CONVERSION;			
	}
	
	private class HumminbirdPing implements Ping {
		
		private int time;
		private int longitude;
		private int latitude;
		private short speed;
		private short heading;
		private byte[] soundings;
		
		public HumminbirdPing(RandomAccessFile stream, int blocksize) throws IOException {
			stream.skipBytes(10);
			time = stream.readInt();
			stream.skipBytes(1);
			longitude = stream.readInt();
			stream.skipBytes(1);
			latitude = stream.readInt();
			stream.skipBytes(3);
			heading = stream.readShort();
			stream.skipBytes(3);
			speed = stream.readShort();
			stream.skipBytes(5);
			int freq = stream.readInt();
			stream.skipBytes(10);
			int son = stream.readInt();
			stream.skipBytes(1);
			
			soundings = new byte[blocksize-58];
			stream.read(soundings, 0, blocksize-58);
		}

		@Override
		public byte[] getSoundings() {
			return soundings;
		}
		
		public void setSoundings(byte[] soundings) {
			this.soundings = soundings;
		}

		@Override
		public float getLowLimit() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public float getTemp() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public float getDepth() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getTimeStamp() {	
			return this.time;
		}

		@Override
		public float getSpeed() {
			return this.speed*3.6f;
		}

		@Override
		public float getTrack() {
			return this.heading/10.0f;
		}

		@Override
		public double getLongitude() {
			return toLongitude(longitude);
		}

		@Override
		public double getLatitude() {
			return toLatitude(latitude);
		}
		
	}

}
