package com.arba.profiler;

import java.util.concurrent.atomic.AtomicLong;

public class App {
    
//    public static AtomicLong aLookupCounter = new AtomicLong();
//    public int _counter;
    
    public static void main( String[] args )
    {
        String result = new App().buildString(120);
        System.out.println("Constructed string of length " + result);
    }

    public String buildString(int length) {
//        ++this._counter;
        String result = "";
        for (int i = 0; i < length; i++) {
            result += (char)(i%26 + 'a');
        }
        return result;
    }

//    public String buildString$impl(int length) {
//        String result = "";
//        for (int i = 0; i < length; i++) {
//            result += (char)(i%26 + 'a');
//        }
//        return result;
//    }
//
//    public String buildString(int length) {
//        long start = System.currentTimeMillis();
//        String result = buildString$impl(length);
//        System.out.println("Call to buildString$impl took " +
//                (System.currentTimeMillis()-start) + " ms.");
//        return result;
//    }
    
}
