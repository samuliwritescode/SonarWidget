package com.vaadin.sonarwidget.widgetset.client.ui;

public class DepthData {
	private String[] depths;
	private String[] temps;
	private String[] lowlimits;
	
	public DepthData(int width) {
		depths = new String[width];
		temps = new String[width];
		lowlimits = new String[width];
	}
	
	private float getFloatValue(String[] table, int index) {
		if(table != null &&
			table.length > index && 
			table[index] != null) {
			return new Float(table[index]).floatValue();
		}
		
		return 0;
	}
	
	public float getDepth(int index) {
		return getFloatValue(this.depths, index);
	}
	
	public float getLowlimit(int index) {
		return getFloatValue(this.lowlimits, index);
	}
	
	public float getTemp(int index) {
		return getFloatValue(this.temps, index);
	}
	
	public void appendDepth(String[] data, int offset) {
		fillArray(data, this.depths, offset);
	}
	
	public void appendLowlimit(String[] data, int offset) {
		fillArray(data, this.lowlimits, offset);
	}
	
	public void appendTemp(String[] data, int offset) {
		fillArray(data, this.temps, offset);
	}
	
	private void fillArray(String[] slice, String[] array, int offset) {
		for(int loop=offset; loop < offset+slice.length && loop < array.length; loop++) {
			array[loop] = slice[loop-offset];
		}	
	}
}
