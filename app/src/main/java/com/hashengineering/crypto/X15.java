package com.hashengineering.crypto;

//import org.bitcoinj.core.Sha256Hash;

//import fr.cryptohash.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Hash Engineering on 4/24/14 for the X15 algorithm
 */
public class X15 {

    private static final Logger log = LoggerFactory.getLogger(X15.class);
    private static boolean native_library_loaded = false;

    static {

        try {
            System.loadLibrary("x15");
            native_library_loaded = true;
        }
        catch(UnsatisfiedLinkError x)
        {

        }
        catch(Exception e)
        {
            native_library_loaded = false;
        }
    }

    public static byte[] x15Digest(byte[] input, int offset, int length)
    {
        return x15_native(input, offset, length);
    }

    public static byte[] x15Digest(byte[] input) {
        //long start = System.currentTimeMillis();
        try {
            return native_library_loaded ? x15_native(input, 0, input.length) : null;
        } catch (Exception e) {
            return null;
        }
        finally {
            //long time = System.currentTimeMillis()-start;
            //log.info("X15 Hash time: {} ms per block", time);
        }
    }

    static native byte [] x15_native(byte [] input, int offset, int length);



}
