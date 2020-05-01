package lowMagicAge;

import java.io.File;

import org.junit.jupiter.api.Test;

public class PersistentMapTest {

	@Test
	public void test() throws Exception {
		File f = new File("build/PersistentMapTest.data");
		f.delete();
		PersistentMap map = new PersistentMap(f);
		map.put(10, 10);
		map.put(20, 20);
		map.put(30,PersistentMap.RESERVED);
		map.remove(20);
		map.persist();
		map.close();
		map = new PersistentMap(f);
		assert map.get(10)==10;
		assert map.get(20)==null;
		assert map.get(30)==null;
	}
	
}
