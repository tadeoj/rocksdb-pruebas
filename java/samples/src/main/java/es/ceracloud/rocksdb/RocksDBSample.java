package es.ceracloud.rocksdb;

import org.rocksdb.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RocksDBSample {
    private static final String dbPath   = "/home/tjulian/temp/rocksdb-data/";
    private static final String cfdbPath = "/home/tjulian/temp/rocksdb-data-cf/";

    static {
        RocksDB.loadLibrary();
    }

    //  RocksDB.DEFAULT_COLUMN_FAMILY
    public void testDefaultColumnFamily() {
        System.out.println("testDefaultColumnFamily begin...");
        //If the file does not exist, create the file first
        try (final Options options = new Options().setCreateIfMissing(true)) {
            try (final RocksDB rocksDB = RocksDB.open(options, dbPath)) {
                //Simple key value
                byte[] key = "Hello".getBytes();
                rocksDB.put(key, "World".getBytes());

                System.out.println(new String(rocksDB.get(key)));

                rocksDB.put("SecondKey".getBytes(), "SecondValue".getBytes());

                //Query primary key through list
                List<byte[]> keys = Arrays.asList(key, "SecondKey".getBytes(), "missKey".getBytes());
                List<byte[]> values = rocksDB.multiGetAsList(keys);
                for (int i = 0; i < keys.size(); i++) {
                    System.out.println("multiGet " + new String(keys.get(i)) + ":" + (values.get(i) != null ? new String(values.get(i)) : null));
                }

                //Print all [key - value]
                RocksIterator iter = rocksDB.newIterator();
                for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                    System.out.println("iterator key:" + new String(iter.key()) + ", iter value:" + new String(iter.value()));
                }

                //Delete a key
                rocksDB.delete(key);
                System.out.println("after remove key:" + new String(key));

                iter = rocksDB.newIterator();
                for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                    System.out.println("iterator key:" + new String(iter.key()) + ", iter value:" + new String(iter.value()));
                }
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    //Using a specific column family to open a database, you can understand a column family as a table in a relational database
    public void testCertainColumnFamily() {
        System.out.println("\ntestCertainColumnFamily begin...");
        try (final ColumnFamilyOptions cfOpts = new ColumnFamilyOptions().optimizeUniversalStyleCompaction()) {
            String cfName = "my-first-columnfamily";
            // list of column family descriptors, first entry must always be default column family
            final List cfDescriptors = Arrays.asList(
                    new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOpts),
                    new ColumnFamilyDescriptor(cfName.getBytes(), cfOpts)
            );

            List<ColumnFamilyHandle> cfHandles = new ArrayList<>();
            try (final DBOptions dbOptions = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
                 final RocksDB rocksDB = RocksDB.open(dbOptions, cfdbPath, cfDescriptors, cfHandles)) {
                ColumnFamilyHandle cfHandle = cfHandles.stream().filter(x -> {
                    try {
                        return (new String(x.getName())).equals(cfName);
                    } catch (RocksDBException e) {
                        return false;
                    }
                }).collect(Collectors.toList()).get(0);

                //Write key / value
                String key = "FirstKey";
                rocksDB.put(cfHandle, key.getBytes(), "FirstValue".getBytes());
                //Query single key
                byte[] getValue = rocksDB.get(cfHandle, key.getBytes());
                System.out.println("get Value : " + new String(getValue));
                //Write the second key / value
                rocksDB.put(cfHandle, "SecondKey".getBytes(), "SecondValue".getBytes());

                List<byte[]> keys = Arrays.asList(key.getBytes(), "SecondKey".getBytes());
                List cfHandleList = Arrays.asList(cfHandle, cfHandle);
                //Query multiple keys
                List<byte[]> values = rocksDB.multiGetAsList(cfHandleList, keys);
                for (int i = 0; i < keys.size(); i++) {
                    System.out.println("multiGet:" + new String(keys.get(i)) + "--" + (values.get(i) == null ? null : new String(values.get(i))));
                }

                //Delete single key
                rocksDB.delete(cfHandle, key.getBytes());

                RocksIterator iter = rocksDB.newIterator(cfHandle);
                for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                    System.out.println("iterator:" + new String(iter.key()) + ":" + new String(iter.value()));
                }
            } finally {
                // NOTE frees the column family handles before freeing the db
                for (final ColumnFamilyHandle cfHandle : cfHandles) {
                    cfHandle.close();
                }
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        } // frees the column family options
    }

    public static void main(String[] args) throws Exception {
        RocksDBSample test = new RocksDBSample();
        test.testDefaultColumnFamily();
        test.testCertainColumnFamily();
    }

}