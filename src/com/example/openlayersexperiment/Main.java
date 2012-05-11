package com.example.openlayersexperiment;

import java.io.File;
import java.io.IOException;

import com.example.openlayersexperiment.LowranceSonar.Ping;



public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String readFrom = "/Users/samuli/Documents/Sonar0001.sl2";
		File file = new File(readFrom);
		
		LowranceSonar sonar = new LowranceSonar(file);
		
		System.out.println("format: "+sonar.getFormat());
		System.out.println("block size: "+sonar.getBlockSize());
		System.out.println("blocks: "+sonar.getLength());
	
		for(int loop=0; loop < 10; loop++) {
			Ping ping = sonar.getPing(loop*100+10000);
			System.out.println("ping: "+ping.toString());
		}
	}

}
