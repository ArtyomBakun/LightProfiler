package com.arba.demo;

public class LeakageApp
{
    public static void main( String[] args )
    {
        System.out.println("*********************************************");
        System.out.println("***Usages of \"com.arba.demo.MemLeak\" class***");
        System.out.println("*********************************************\n");
        System.out.println("com.arba.demo.MyController#generateMapWithSize     43592");
        System.out.println("com.arba.demo.MyController#convertStringArrayToMemLeakList     634");
        System.out.println("com.arba.demo.MemLeakList#getRandomList     249");
        System.out.println("com.arba.demo.MemLeak#clone     21");
        System.out.println("com.arba.demo.MyController#randomize     14");
        System.out.println("com.arba.demo.MyController#sort     14");
        System.out.println("\n*********************************************");
    }
}
