package com.example.openlayersexperiment;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.vaadin.Application;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.StreamResource;
import com.vaadin.ui.*;

public class OpenlayersexperimentApplication extends Application {
	
	private final int height = 400;
	private final int width = 600;
	private VerticalLayout layout;
	private LowranceSonar sonar;
	private BufferedImage image;
	
	
	@Override
	public void init() {
		Window mainWindow = new Window("Openlayersexperiment Application");
		
		image = new BufferedImage (width, height, BufferedImage.TYPE_INT_RGB);
		layout = new VerticalLayout();
		try {
			sonar = new LowranceSonar(new File("/Users/samuli/Documents/Sonar0011.slg"));
		} catch (IOException e) {
			mainWindow.showNotification(e.toString());
			e.printStackTrace();
		}
		
		Slider slider = new Slider(0, (int) sonar.getLength());
		slider.setWidth("100%");
		mainWindow.addComponent(slider);
		mainWindow.addComponent(layout);
		
		setMainWindow(mainWindow);
		redraw(0);
		slider.addListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				Double index = (Double)event.getProperty().getValue();
				redraw(index.intValue());
			}
		});
		slider.setImmediate(true);
	}
	
	private void redraw(int index) {
		layout.removeAllComponents();
		LowranceSonar.Ping firstping = createSonarImage(index);
		layout.addComponent(
			new Link(
				"depth: "+firstping.getDepth()+
				", temp: "+firstping.getTemp()+
				", speed: "+firstping.getSpeed()+
				", time: "+firstping.getTimeStamp()/1000/60, 
				new ExternalResource(
					String.format(
						"http://maps.googleapis.com/maps/api/staticmap?center=%f,%f&zoom=14&size=800x800&sensor=false",
						firstping.getLatitude(),
						firstping.getLongitude()
					)
				)
			)
		);
				
		layout.addComponent(
			new Embedded(
					"",
					new StreamResource(streamresource,
							Double.toString(Math.random())+"image.jpg",
							this
					)
			)
		);
	}
	
	private StreamResource.StreamSource streamresource = new StreamResource.StreamSource() {

		@Override
		public InputStream getStream() {
			
	        try {
	        	ByteArrayOutputStream imagebuffer = new ByteArrayOutputStream();
	            ImageIO.write(image, "jpeg", imagebuffer);	           
	            return  new ByteArrayInputStream(imagebuffer.toByteArray());
	        } catch (IOException e) {
	        	return null;
	        }
		}
    };


	private LowranceSonar.Ping createSonarImage(int index) {
		LowranceSonar.Ping retval = null;
		try {
			LowranceSonar.Ping[] pings = sonar.getPingRange(index, width);
	
			for(int loop=0; loop < width; loop++) {
				if(retval == null) {
					retval = pings[loop];
				}
				
				byte[] soundings = pings[loop].getSoundings();
				
				for(int i=0; i < height; i++) {
					byte sounding = soundings[i*(soundings.length/height)];
					int color = (0xFF&sounding) |
							(0xFF00&(sounding<<7)) |
							(0xFF0000&(sounding<<17));
					image.setRGB(loop, i, color);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return retval;
	}

}
