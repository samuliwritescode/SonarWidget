package com.vaadin.sonarwidget.data;

public interface Ping {
	byte[] getSoundings();
	float getLowLimit();
	float getTemp();
	float getDepth();
	int getTimeStamp();
	float getSpeed();
	float getTrack();
	double getLongitude();
	double getLatitude();
}
