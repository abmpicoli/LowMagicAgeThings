package lowMagicAge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ReadMap {

	
	public static void main(String[] args) throws Exception {
		
		File directory = General.getMainDirectory();
		File worldFile = new File(directory,"wlds\\wld_1_sites.txt");
		File outputFile = new File(directory,"wlds\\wld_1_sites.tsp");
		//wsites v1 329 // id type img_var img_flip x y[ en_name,cs_name]
		try(FileReader fr = new FileReader(worldFile);
				BufferedReader reader = new BufferedReader(fr)) {
		
		}
	}
}
