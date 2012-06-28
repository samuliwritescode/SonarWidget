package com.vaadin.sonarwidget;

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

import com.vaadin.sonarwidget.LowranceSonar.Ping;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.StreamResource.StreamSource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.ClientWidget;

@ClientWidget(com.vaadin.sonarwidget.widgetset.client.ui.VSonarWidget.class)
public class SonarWidget extends AbstractComponent{

	private LowranceSonar sonar;
	private Queue<Frame> offsets;
	private boolean overlay = true;
	
	private static class Frame {
		public Integer offset;
		public Integer width;
		public Integer height;
	}
	
	public SonarWidget(File file) {
		offsets = new LinkedList<Frame>();
		try {
			sonar = new LowranceSonar(file);
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
			
			String[] lowlimits = new String[frame.width];
			String[] depths = new String[frame.width];
			String[] temps = new String[frame.width];
			Ping[] pingRange = null;
			try {
				if(sonar.getLength() > (frame.offset+frame.width)) {
					pingRange = sonar.getPingRange(frame.offset, frame.width);
				} else {
					pingRange = sonar.getPingRange(frame.offset, (int) (sonar.getLength()-frame.offset));
				}
				
			} catch (IOException e) {		
				e.printStackTrace();
			}
			
			for(int loop=0; loop < frame.width && loop < pingRange.length; loop++) {
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
			            ImageIO.write(Main.createImage(sonar, frame.offset, frame.width, frame.height), "png", imagebuffer);	           
			            return new ByteArrayInputStream(imagebuffer.toByteArray());
			        } catch (IOException e) {
			        	e.printStackTrace();
			        	return null;
			        }
				}
			}, 
			String.format("frame%d-%d.png", frame.offset, new Date().getTime()), 
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
}
