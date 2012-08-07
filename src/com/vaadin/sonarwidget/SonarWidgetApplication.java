package com.vaadin.sonarwidget;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.sonarwidget.data.Sonar.Type;
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
		selector.addItem("SideScan Sonar0001.sl2");
		selector.addItem("DownScan Sonar0001.sl2");
		selector.addItem("2D Sonar0011.slg");
		selector.addItem("SideScan R00001.DAT");
		selector.addItem("DownScan R00001.DAT");
		selector.addListener(new Property.ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				sonarLayout.removeAllComponents();
				String selected = (String)event.getProperty().getValue();
				
				Pattern pattern = Pattern.compile("\\S+");
				Matcher matcher = pattern.matcher(selected);
				matcher.find();
				Type type = matcher.group().equalsIgnoreCase("SideScan")?Type.eSideScan:Type.eDownScan;
				matcher.find();
				String filename = matcher.group();
				
				SonarWidget sonarWidget = new SonarWidget(new File("/Users/samuli/sonar/"+filename), type);

				sonarWidget.setHeight("300px");
				sonarWidget.setWidth("100%");
				sonarLayout.addComponent(sonarWidget);
			}
		});
		
		selector.select("SideScan Sonar0001.sl2");
		
		
		layout.addComponent(selector);
		layout.addComponent(sonarLayout);
		mainWindow.addComponent(layout);
		setMainWindow(mainWindow);	
	}
}
