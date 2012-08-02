package com.vaadin.sonarwidget;

import java.io.IOException;

public interface Sonar {

	long getLength();
	Ping[] getPingRange(int index, int length) throws IOException;
}
