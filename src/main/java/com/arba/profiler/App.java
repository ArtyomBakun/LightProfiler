package com.arba.profiler;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by artyom on 28.03.17.
 */
public class App {
    static final AtomicLong lookupAccountCalls = new AtomicLong();
    public static void main( String[] args )
    {
        lookupAccountCalls.incrementAndGet();
        A a = new A();
        System.out.println( "Hello World! App" );
    }
 
}
class A{}
