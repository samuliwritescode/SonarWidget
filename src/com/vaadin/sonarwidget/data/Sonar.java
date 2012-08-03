package com.vaadin.sonarwidget.data;

import java.io.IOException;

public interface Sonar {

	long getLength();
	Ping[] getPingRange(int index, int length) throws IOException;
	Type getType();
	
	public enum Type {
		eTraditional, 
		eDownScan, 
		eSideScan
	}
}
