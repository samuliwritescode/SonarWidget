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
