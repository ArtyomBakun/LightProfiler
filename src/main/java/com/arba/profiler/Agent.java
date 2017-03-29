package com.arba.profiler;

import java.lang.instrument.Instrumentation;
import java.io.IOException;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.Type;

import static org.apache.bcel.Constants.*;

public class Agent
{
    public static void premain(String agentArgs, Instrumentation instrumentation)
    {
        System.out.println("Hello World! Java Agent");
//        createSyntheticClass();
        modifyAppClass();
    }
    
    private static void modifyAppClass(){
        try
        {
            JavaClass appclass = Repository.lookupClass("com.arba.profiler.App");
            printCode(appclass);
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }
    public static void printCode(JavaClass appclass) {
        ClassGen cg = new ClassGen(appclass);
        for (Method method : appclass.getMethods()) {
//            System.out.println(method);
            Code code = method.getCode();
            MethodGen mg = new MethodGen(method, cg.getClassName(), cg.getConstantPool());
            InstructionList il = mg.getInstructionList();
            for (InstructionHandle ih : il.getInstructionHandles())
            {
                Instruction i = ih.getInstruction();
                if (i instanceof LDC)
                {
//                    System.out.println("\n#FFFFF#\n"+code+"\n#DDDDD#\n");
                    try
                    {
                        LDC value = (LDC) i;
//                        System.out.println("Found LDC instruction");
//                        System.out.println(cg.getConstantPool());
                        il.insert(value, new LDC(cg.getConstantPool().addString("Hello World! Modified App")));
//                        System.out.println(cg.getConstantPool());
                        il.delete(value);
                        mg.setMaxLocals();
                        mg.setMaxStack();
                        mg.setInstructionList(il);
                        cg.replaceMethod(method, mg.getMethod());
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
//                    System.out.println("\n#FFFFF#\n"+code+"\n#DDDDD#\n");
                }
                if (i instanceof NEW)
                {
                    //                    System.out.println("\n#FFFFF#\n"+code+"\n#DDDDD#\n");
                    try
                    {
                        NEW value = (NEW) i;
                        //                        System.out.println("Found LDC instruction");
                        //                        System.out.println(cg.getConstantPool());
                        if(value.getType(cg.getConstantPool()).getSignature().equals("Lcom/arba/profiler/A;"));
//                        il.insert(value, new LDC(cg.getConstantPool().addString("Hello World! Modified App")));
//                        //                        System.out.println(cg.getConstantPool());
//                        il.delete(value);
//                        mg.setMaxLocals();
//                        mg.setMaxStack();
//                        mg.setInstructionList(il);
//                        cg.replaceMethod(method, mg.getMethod());
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    //                    System.out.println("\n#FFFFF#\n"+code+"\n#DDDDD#\n");
                }
            }
            try
            {
                cg.getJavaClass().dump("com/arba/profiler/App.class");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    private static void createSyntheticClass(){
        System.out.println("Generating Class");

        final String fullClassname = "com.arba.profiler.SyntheticClass";
        //Create a ClassGen for our brand new class.
        ClassGen classGen = new ClassGen(fullClassname, "java.lang.Object", "SyntheticClass.java",
                ACC_PUBLIC, null);

        //Get a reference to the constant pool of the class.
        // This will be modified as we add methods, fields etc. Note that it already constains
        //a few constants.
        ConstantPoolGen constantPoolGen = classGen.getConstantPool();

        System.out.println(fullClassname.replace(".", "/"));

        classGen.addEmptyConstructor(ACC_PUBLIC);

        //The list of instructions for a method. 
        InstructionList instructionList = new InstructionList();

        //Add the appropriate instructions.

        //Get the reference to static field out in class java.lang.System.
        instructionList
                .append(new GETSTATIC(constantPoolGen.addFieldref("java.lang.System", "out", "Ljava/io/PrintStream;")));

        //Load the constant
        instructionList.append(new LDC(constantPoolGen.addString(" You are a real geek!")));

        //Invoke the method.
        instructionList.append(new INVOKEVIRTUAL(
                constantPoolGen.addMethodref("java.io.PrintStream", "println", "(Ljava/lang/String;)V")));

        //Return from the method
        instructionList.append(new RETURN());

        MethodGen methodGen = new MethodGen(ACC_PUBLIC | ACC_STATIC, Type.VOID,
                new Type[] { new ArrayType(Type.STRING, 1) }, new String[] { "args" }, "main",
                fullClassname, instructionList, constantPoolGen);

        methodGen.setMaxLocals();//Calculate the maximum number of local variables. 
        methodGen.setMaxStack();//Very important: must calculate the maximum size of the stack.

        classGen.addMethod(methodGen.getMethod()); //Add the method to the class

        //Print a few things.
        System.out.println("********Constant Pool**********");
        System.out.println(constantPoolGen.getFinalConstantPool());
        System.out.println("********Method**********");
        System.out.println(methodGen);
        System.out.println("********Instruction List**********");
        System.out.println(instructionList);

        //Now generate the class java -javaagent:profiler-0.1-jar-with-dependencies.jar com.arba.profiler.App
        //        java -javaagent:profiler-0.1.jar com.arba.profiler.App
        JavaClass javaClass = classGen.getJavaClass();
        try
        {
            //Write the class byte code into a file
            javaClass.dump(fullClassname.replace(".", "/") +".class");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
