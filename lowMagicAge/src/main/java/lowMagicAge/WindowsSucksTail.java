package lowMagicAge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;

public class WindowsSucksTail {

	public static void main(String[] args) throws Exception {
		File f = new File(args[0]);
			
		while(true) {
			try(FileInputStream fi = new FileInputStream(f);
					InputStreamReader reader = new InputStreamReader(fi);
					BufferedReader br = new BufferedReader(reader);
					) {
				fi.getChannel().position(f.length()-1000000);
				String line;
				do {
					line = br.readLine();
					if(line != null) {
						System.out.println(line);					
					} else {
						Thread.sleep(500);
					}
					
				} while(true);
			}
		
		}
	}
}
