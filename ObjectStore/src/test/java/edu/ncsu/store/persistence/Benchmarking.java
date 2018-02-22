package edu.ncsu.store.persistence;

import edu.ncsu.chord.ChordID;
import edu.ncsu.store.KeyMetadata;

public class Benchmarking {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//put();
		getLatest();
		//getAll();
	}
	
	public static void put(){
		try {
			ImmutableStore store = new ImmutableStore();
			KeyMetadata key = new KeyMetadata(new ChordID<String>("put1"));
			byte[] bytes = new byte[4096];
			long startTime;
			for(int i = 0; i < 1000; i++){
				startTime = System.nanoTime();
				store.put(key, bytes);
				System.out.println(System.nanoTime() - startTime);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void getLatest(){
		try {
			ImmutableStore store = new ImmutableStore();
			KeyMetadata key = new KeyMetadata(new ChordID<String>("getKey2"));
			byte[] bytes = new byte[4096];
			long startTime;
			for(int i = 0; i < 1000; i++){
				store.put(key, bytes);
				startTime = System.nanoTime();
				store.get("getKey2");
				System.out.println(System.nanoTime() - startTime);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public static void getAll(){
		try {
			ImmutableStore store = new ImmutableStore();
			KeyMetadata key = new KeyMetadata(new ChordID<String>("rangeKey"));
			byte[] bytes = new byte[4096];
			long startTime;
			for(int i = 0; i < 1000; i++){
				store.put(key, bytes);
				startTime = System.nanoTime();
				store.get("rangeKey", 0L, System.currentTimeMillis());
				System.out.println(System.nanoTime() - startTime);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
