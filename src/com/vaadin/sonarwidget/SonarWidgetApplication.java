package com.vaadin.sonarwidget;
import java.io.File;

import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.*;

public class SonarWidgetApplication extends Application {
	
	@Override
	public void init() {
		Window mainWindow = new Window("Sonarwidget Application");

		VerticalLayout layout = new VerticalLayout();
		final VerticalLayout sonarLayout = new VerticalLayout();
		ComboBox selector = new ComboBox("Select file");
		
		selector.setImmediate(true);
		selector.setNullSelectionAllowed(false);
		selector.addItem("/Users/samuli/sonar/Sonar0001.sl2");
		selector.addItem("/Users/samuli/sonar/Sonar0011.slg");
		selector.addListener(new Property.ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				sonarLayout.removeAllComponents();
				String filename = (String)event.getProperty().getValue();
				SonarWidget sonarWidget = new SonarWidget(new File(filename));

				sonarWidget.setHeight("300px");
				sonarWidget.setWidth("100%");
				sonarLayout.addComponent(sonarWidget);
			}
		});
		
		selector.select("/Users/samuli/sonar/Sonar0001.sl2");
		
		
		layout.addComponent(selector);
		layout.addComponent(sonarLayout);
		mainWindow.addComponent(layout);
		setMainWindow(mainWindow);	
	}
}
