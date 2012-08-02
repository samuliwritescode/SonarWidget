package com.vaadin.sonarwidget;

public interface Ping {
	byte[] getSoundings();
	float getLowLimit();
	float getTemp();
	float getDepth();
}
