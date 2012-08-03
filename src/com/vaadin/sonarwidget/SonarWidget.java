package com.vaadin.sonarwidget;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.imageio.ImageIO;

import com.vaadin.sonarwidget.data.LowranceSonar;
import com.vaadin.sonarwidget.data.LowranceStructureScan;
import com.vaadin.sonarwidget.data.Ping;
import com.vaadin.sonarwidget.data.Sonar;
import com.vaadin.sonarwidget.data.Sonar.Type;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.StreamResource.StreamSource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.ClientWidget;

@ClientWidget(com.vaadin.sonarwidget.widgetset.client.ui.VSonarWidget.class)
public class SonarWidget extends AbstractComponent{

	private Sonar sonar;
	private Queue<Frame> offsets;
	private boolean overlay = true;
	
	private static class Frame {
		public Integer offset;
		public Integer width;
		public Integer height;
	}
	
	public SonarWidget(File file, Type preferredChannel) {
		offsets = new LinkedList<Frame>();
		try {
			String filenameExtension = file.getName().substring(file.getName().length()-3);
			if(filenameExtension.equalsIgnoreCase("sl2")) {
				sonar = new LowranceStructureScan(file, preferredChannel);
			} else if(filenameExtension.equalsIgnoreCase("slg")) {
				sonar = new LowranceSonar(file);
			}
		} catch (IOException e) {			
			throw new RuntimeException(e);
		}

	}
	
	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target);
		
		target.addAttribute("overlay", overlay);
		final Frame frame = this.offsets.poll();			

		if(frame != null && 
			frame.width != null && 
			frame.height != null && 
			frame.offset != null) {
			if(sonar.getLength() < frame.offset) {
				return;
			}
			
			if(sonar.getLength() < (frame.offset+frame.width)) {
				frame.width = (int) (sonar.getLength() - frame.offset);
			}
			
			Ping[] pingRange = null;
			try {
				pingRange = sonar.getPingRange(frame.offset, frame.width);
			} catch (IOException e) {		
				e.printStackTrace();
			}
			
			String[] lowlimits = new String[pingRange.length];
			String[] depths = new String[pingRange.length];
			String[] temps = new String[pingRange.length];
			
			for(int loop=0; loop < pingRange.length; loop++) {
				lowlimits[loop] = String.format("%.1f", pingRange[loop].getLowLimit());	
				depths[loop] = String.format("%.1f", pingRange[loop].getDepth());
				temps[loop] = String.format("%.1f", pingRange[loop].getTemp());
			}

			target.addAttribute("pingcount", sonar.getLength());
			target.addAttribute("lowlimits", lowlimits);
			target.addAttribute("depths", depths);
			target.addAttribute("temps", temps);
			target.addAttribute("offset", frame.offset);
						
			StreamResource streamResource = new StreamResource(new StreamSource() {
				@Override
				public InputStream getStream() {
			        try {
			        	ByteArrayOutputStream imagebuffer = new ByteArrayOutputStream();
			            ImageIO.write(createImage(sonar, frame.offset, frame.width, frame.height), "jpg", imagebuffer);	           
			            return new ByteArrayInputStream(imagebuffer.toByteArray());
			        } catch (IOException e) {
			        	e.printStackTrace();
			        	return null;
			        }
				}
			}, 
			String.format("frame%d-%d.jpg", frame.offset, new Date().getTime()), 
			getApplication()
			);
			
			target.addAttribute("pic", streamResource);
		}
	}

	@Override
	public void changeVariables(Object source, Map<String, Object> variables) {
		Frame frame = new Frame();
		if(variables.containsKey("windowwidth")) {
			frame.width = (Integer)variables.get("windowwidth");			
		}
		
		if(variables.containsKey("windowheight")) {
			frame.height = (Integer)variables.get("windowheight");
		}
		
		if(variables.containsKey("currentwindow")) {
			frame.offset = (Integer)variables.get("currentwindow");
		}
		this.offsets.add(frame);
		
		requestRepaint();
	}

	public void setOverlay(boolean booleanValue) {
		this.overlay = booleanValue;
		requestRepaint();
	}
	
	private BufferedImage createImage(Sonar sonar, int offset, int width, int height) {
		BufferedImage image = new BufferedImage (width, height, BufferedImage.TYPE_INT_RGB);
		
		try {
			Ping[] pings = sonar.getPingRange(offset, width);
	
			for(int loop=0; loop < width; loop++) {
				
				byte[] soundings = pings[loop].getSoundings();
				
				for(int i=0; i < height; i++) {
					int mapped = (int)((i*soundings.length)/(double)height);
					byte sounding = soundings[mapped];
					int color = (0xFF&sounding) |
							(0xFF00&(sounding<<8)) |
							(0xFF0000&(sounding<<16));
					image.setRGB(loop, i, color);
				}								
			}
		} catch (IOException e) {
			return image;
		}	
		
		return image;
	}
}
