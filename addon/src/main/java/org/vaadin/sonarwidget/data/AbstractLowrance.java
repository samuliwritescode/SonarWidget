package org.vaadin.sonarwidget.data;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Lowrance formats have same endianess and 
 * units for data. This is combined here. 
 * @author samuli
 *
 */
public abstract class AbstractLowrance implements Sonar {
	
	private static final double RAD_CONVERSION = 180/Math.PI;
	private static final double EARTH_RADIUS = 6356752.3142;
	private static final float FEET_TO_METERS = 0.3048f;
	private static final float KNOTS = 1.852f;
	
	protected byte[] readBytes(RandomAccessFile stream, int len) throws IOException {
		byte[] bytes = new byte[len];
		stream.read(bytes, 0, len);
		return bytes;
	}
	
	protected byte[] readBytes(DataInputStream stream, int len) throws IOException {
		byte[] bytes = new byte[len];
		stream.read(bytes, 0, len);
		return bytes;
	}
	
	protected int toBigEndianInt(byte[] raw, int offset) {
		return 0xFF000000&(raw[offset+3]<<24) | 0x00FF0000&(raw[offset+2]<<16) | 0x0000FF00&(raw[offset+1]<<8) | 0x000000FF&raw[offset];
	}
	
	protected float toBigEndianFloat(byte[] raw, int offset) {
		return Float.intBitsToFloat(toBigEndianInt(raw, offset));
	}
	
	protected short toBigEndianShort(short littleendian) {
		return (short) (((0xFF00&littleendian)>>8)&0x00FF |
				((0x00FF&littleendian)<<8)&0xFF00);
	}
	
	protected float toMetres(float feets) {
		return feets*FEET_TO_METERS;
	}
	
	protected float toKilometersPerHour(float knots) {
		return knots*KNOTS;
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
}
