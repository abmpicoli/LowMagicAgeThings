package lowMagicAge;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

public class PersistentMap implements Map<Integer, Double>, AutoCloseable {

	/**
	 * Represents that the double in question is being calculated and shouldn't be
	 * used or stored.
	 */
	public static final double RESERVED = Double.MIN_VALUE;
	public static final double DELETED = Double.MAX_VALUE * 0.99;
	private HashMap<Integer, Double> wrapper = new HashMap<>();
	/**
	 * The cache contains stuff to be persisted. the special value Double.NaN means
	 * that a key must be removed.
	 */
	private HashMap<Integer, Double> cache = new HashMap<>();
	double lastUpdate;
	private File file;

	/**
	 * Gets a persistent map backed by a file. There must be only ONE persistent map
	 * acting on a single file at any times. Or else the results will be
	 * unpredictable.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public PersistentMap(File file) {
		this.file = file;
		if (file.exists() && file.isFile()) {
			File backup = new File(file.getParentFile(),
					file.getName() + ".bk" + new SimpleDateFormat("yyMMdd-HHmmss.SSS").format(new Date()));
			try {
				Files.copy(file.toPath(), backup.toPath());
				try (FileInputStream fin = new FileInputStream(file); DataInputStream din = new DataInputStream(fin)) {
					int count = 0;
					do {
						try {
							count = din.readInt();
						} catch (EOFException ex) {
							System.err.println("Eof");
							return;
						}
						if (count > 0) {
							for (int i = 0; i < count; ++i) {
								int key = din.readInt();
								double value = din.readDouble();
								if (value == DELETED) {
									wrapper.remove(key);
								} else if (value != RESERVED) {
									wrapper.put(key, value);
								}
							}
						}
					} while (count > 0);

				} finally {
					System.out.println("READ: " + wrapper);
					if (!cache.isEmpty()) {
						file.delete();
						try {
							persist();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
				cache.putAll(wrapper);

			}
		}

	}

	public synchronized void persist() throws IOException {
		if (cache.size() == 0) {
			System.out.println("Nothing to persist");
			return;
		}
		try (FileOutputStream fout = new FileOutputStream(file, true);
				DataOutputStream dout = new DataOutputStream(fout)) {

			dout.writeInt(cache.size());

			for (Entry<Integer, Double> x : cache.entrySet()) {
				dout.writeInt(x.getKey());
				dout.writeDouble(x.getValue());
			}
			System.out.println("Persisted: " + cache);
			cache.clear();
		}
	}

	@Override
	public Set<Entry<Integer, Double>> entrySet() {
		return wrapper.entrySet();
	}

	@Override
	public int size() {
		return wrapper.size();
	}

	@Override
	public boolean isEmpty() {
		return wrapper.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return wrapper.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return wrapper.containsValue(value);

	}

	@Override
	public Double get(Object key) {
		Double result = wrapper.get(key);
		if (result != null && !(result == DELETED)) {
			return result;
		} else {
			return null;
		}
	}

	@Override
	public synchronized Double put(Integer key, Double value) {
		Objects.requireNonNull(value);
		Double result = wrapper.put(key, value);
		if (value != RESERVED) {
			cache.put(key, value);
		}
		return result;
	}

	@Override
	public Double remove(Object key) {
		if (!(key instanceof Integer)) {
			return null;
		}
		Double result = wrapper.remove(key);
		cache.put((Integer) key, DELETED);
		return result;
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends Double> m) {
		for (Entry<? extends Integer, ? extends Double> x : m.entrySet()) {
			put(x.getKey(), x.getValue());
		}

	}

	@Override
	public synchronized void clear() {
		wrapper.clear();
		cache.clear();
		if (file.isFile()) {
			file.delete();
		}
	}

	@Override
	public Set<Integer> keySet() {
		return Collections.unmodifiableSet(wrapper.keySet());
	}

	@Override
	public Collection<Double> values() {
		return Collections.unmodifiableCollection(wrapper.values());
	}

	@Override
	public synchronized void close() throws Exception {
		persist();
		wrapper = null;
		cache = null;
	}

	public void put(Integer key, Integer value) {
		Objects.requireNonNull(value);
		put(key, new Double(value));

	}

}
