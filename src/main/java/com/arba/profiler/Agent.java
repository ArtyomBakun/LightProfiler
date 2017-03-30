package com.arba.profiler;

import java.io.FileOutputStream;
import java.lang.instrument.Instrumentation;
import java.io.IOException;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

import static org.apache.bcel.Constants.*;

public class Agent
{
    public static void premain(String agentArgs, Instrumentation instrumentation)
    {
//        createSyntheticClass();
        modifyAppClass();
    }
    
    private static void modifyAppClass(){
        try
        {
            JavaClass appclass = Repository.lookupClass("com.arba.profiler.App");
//            modifyHelloWorld(appclass);
            wrapMethod(appclass, "buildString");
//            printClass(appclass);
        }
        catch (ClassNotFoundException | IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void printClass(JavaClass appclass) {
        for (Method method : appclass.getMethods())
        {
            System.out.println(method.getName() + "\n" + method.getCode() + "\n****************\n\n");
        }
    }

    private static void wrapMethod(JavaClass appclass, String methodName) throws IOException
    {
        for (Method method : appclass.getMethods())
        {
            if(method.getName().equals(methodName)){
                addWrapper(new ClassGen(appclass), method);
                break;
            }
        }
    }
    
    public static void modifyHelloWorld(JavaClass appclass) throws TargetLostException, IOException
    {
        ClassGen cg = new ClassGen(appclass);
        for (Method method : appclass.getMethods()) {
            MethodGen mg = new MethodGen(method, cg.getClassName(), cg.getConstantPool());
            InstructionList il = mg.getInstructionList();
            for (InstructionHandle ih : il.getInstructionHandles())
            {
                Instruction i = ih.getInstruction();
                if (i instanceof LDC)
                {
                    LDC value = (LDC) i;
                    il.insert(value, new LDC(cg.getConstantPool().addString("Hello World! Modified App")));
                    il.delete(value);
                    mg.setMaxLocals();
                    mg.setMaxStack();
                    mg.setInstructionList(il);
                    cg.replaceMethod(method, mg.getMethod());
                }
                if (i instanceof NEW)
                {
                    NEW value = (NEW) i;
                    if(value.getType(cg.getConstantPool()).getSignature().equals("Lcom/arba/profiler/A;"));
                }
            }
        }
        appclass.dump("com/arba/profiler/App.class");
    }

    private static void addWrapper(ClassGen cgen, Method method) throws IOException
    {
        // set up the construction tools
        InstructionFactory ifact = new InstructionFactory(cgen);
        InstructionList ilist = new InstructionList();
        ConstantPoolGen pgen = cgen.getConstantPool();
        String cname = cgen.getClassName();
        MethodGen wrapgen = new MethodGen(method, cname, pgen);
        wrapgen.setInstructionList(ilist);

        // rename a copy of the original method
        MethodGen methgen = new MethodGen(method, cname, pgen);
        cgen.removeMethod(method);
        String iname = methgen.getName() + "$impl";
        methgen.setName(iname);
        cgen.addMethod(methgen.getMethod());
        Type result = methgen.getReturnType();

        // compute the size of the calling parameters
        Type[] types = methgen.getArgumentTypes();
        int slot = methgen.isStatic() ? 0 : 1;
        for (int i = 0; i < types.length; i++) {
            slot += types[i].getSize();
        }

        // save time prior to invocation
        ilist.append(ifact.createInvoke("java.lang.System", "currentTimeMillis", Type.LONG, Type.NO_ARGS, INVOKESTATIC));
        ilist.append(InstructionFactory.createStore(Type.LONG, slot));

        // call the wrapped method
        int offset = 0;
        short invoke = INVOKESTATIC;
        if (!methgen.isStatic()) {
            ilist.append(InstructionFactory.createLoad(Type.OBJECT, 0));
            offset = 1;
            invoke = INVOKEVIRTUAL;
        }
        System.out.println("invoke=" + invoke);
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            ilist.append(InstructionFactory.createLoad(type, offset));
            offset += type.getSize();
        }
        ilist.append(ifact.createInvoke(cname, iname, result, types, invoke));

        // store result for return later
        if (result != Type.VOID) {
            ilist.append(InstructionFactory.createStore(result, slot+2));
        }

        // print time required for method call
        ilist.append(ifact.createFieldAccess("java.lang.System", "out", 
                new ObjectType("java.io.PrintStream"), GETSTATIC));
//        ilist.append(InstructionConstants.DUP);
        ilist.append(InstructionConstants.DUP);
        String text = "Call to method " + methgen.getName() + " took ";
        ilist.append(new PUSH(pgen, text));
        ilist.append(ifact.createInvoke("java.io.PrintStream", "print", 
                Type.VOID, new Type[] { Type.STRING }, INVOKEVIRTUAL));
        ilist.append(ifact.createInvoke("java.lang.System", "currentTimeMillis", 
                Type.LONG, Type.NO_ARGS, INVOKESTATIC));
        ilist.append(InstructionFactory.createLoad(Type.LONG, slot));
        ilist.append(InstructionConstants.LSUB);
        ilist.append(ifact.createInvoke("java.io.PrintStream", "print", 
                Type.VOID, new Type[] { Type.LONG }, INVOKEVIRTUAL));
        ilist.append(new PUSH(pgen, " ms."));
        ilist.append(ifact.createInvoke("java.io.PrintStream", "println", 
                Type.VOID, new Type[] { Type.STRING }, INVOKEVIRTUAL));

        // return result from wrapped method call
        if (result != Type.VOID) {
            ilist.append(InstructionFactory.
                    createLoad(result, slot+2));
        }
        ilist.append(InstructionFactory.createReturn(result));

        System.out.println(wrapgen.getMethod().getCode());

        // finalize the constructed method
        wrapgen.stripAttributes(true);
        wrapgen.setMaxStack();
        wrapgen.setMaxLocals();
        cgen.addMethod(wrapgen.getMethod());
        ilist.dispose();
        cgen.getJavaClass().dump("com/arba/profiler/App.class");
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
