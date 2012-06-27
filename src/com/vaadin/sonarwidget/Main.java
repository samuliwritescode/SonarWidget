package com.vaadin.sonarwidget;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.vaadin.sonarwidget.LowranceSonar.Ping;



public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String readFrom = "/Users/samuli/Documents/Sonar0011.slg";
		File file = new File(readFrom);
		
		final LowranceSonar sonar = new LowranceSonar(file);
		
		System.out.println("format: "+sonar.getFormat());
		System.out.println("block size: "+sonar.getBlockSize());
		System.out.println("blocks: "+sonar.getLength());
	
		Ping[] pingRange = sonar.getPingRange(0, 10);
		for(Ping ping: pingRange) {
			System.out.println("ping: "+ping.toString());
		}
		
		JFrame frame = new JFrame("Sonar viewer");
		
		final JLabel label = new JLabel();
		label.setIcon(new ImageIcon(createImage(sonar, 0, 600, 200)));
		
		JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, (int) sonar.getLength(), 0);
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {				
				JSlider c = (JSlider)arg0.getSource();
				label.setIcon(new ImageIcon(createImage(sonar, c.getValue(), 600, 200)));
				
			}});
		
		JPanel panel = new JPanel(new BorderLayout());
		
		panel.add(label, BorderLayout.CENTER);
		panel.add(slider, BorderLayout.NORTH);
		
		frame.add(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	public static BufferedImage createImage(LowranceSonar sonar, int offset, int width, int height) {
		BufferedImage image = new BufferedImage (width, height, BufferedImage.TYPE_INT_RGB);
		
		try {
			LowranceSonar.Ping[] pings = sonar.getPingRange(offset, width);
	
			for(int loop=0; loop < width; loop++) {
				
				byte[] soundings = pings[loop].getSoundings();
				
				for(int i=0; i < height; i++) {
					byte sounding = soundings[i*(soundings.length/height)];
					int color = (0xFF&sounding) |
							(0xFF00&(sounding<<8)) |
							(0xFF0000&(sounding<<16));
					image.setRGB(loop, i, color);
				}
				//image.setRGB(loop, (int)(height*pings[loop].getDepth()/pings[loop].getLowLimit()), 0x0000FF00);								
			}
			/*Graphics graphics = image.getGraphics();
			graphics.setColor(Color.black);
			graphics.drawString(String.format("depth: %02f", pings[0].getDepth()), 10, 10);
			graphics.drawString(String.format("pos: %f lat, %f lon", pings[0].getLatitude(), pings[0].getLongitude()), 10, 30);
			graphics.drawString(String.format("time %d", pings[0].getTimeStamp()/1000), 10, 50);
			graphics.drawString(String.format("temp: %02f", pings[0].getTemp()), 10, 70);
			graphics.drawString(String.format("track: %02f", 54*pings[0].getTrack()), 10, 90);
			graphics.drawString(String.format("speed: %02f", pings[0].getSpeed()), 10, 110);
			graphics.dispose();*/
		} catch (IOException e) {
			return image;
		}	
		
		return image;
	}

}
