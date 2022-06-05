package tsn_java_native_system;

import com.sun.jna.Library;
import com.sun.jna.Native;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.SymbolLookup;

public class TSN_JAVA_NATIVE_SYSTEM {

    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();

    public interface CStdLib extends Library {

        int syscall(int number, Object... args);
    }

    public static void main(String[] args) {
        if (OS.equals("linux") && OS_ARCH.contains("64")) { // linux && amd64
            runtimeDemo();
            jnaNativeDemo();
            incubatorForeignDemo();
        } else {
            System.out.println("Неподдерживаемая ОС для jnaNativeDemo, только для Linux x64 !");
        }
    }

    // Пример работы с Runtime
    public static void runtimeDemo() {
        Runtime r = Runtime.getRuntime();
        ArrayList l = new ArrayList();
        try {
            Process p = r.exec(new String[]{"bash", "-c", "ls ./dist/ -R"});
            //Process p = r.exec("ls -l");
            BufferedReader bufferedreader = new BufferedReader(
                    new InputStreamReader(new BufferedInputStream(p.getInputStream())));
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                l.add(line + "\n");
            }
            try {
                if (p.waitFor() != 0) {
                    System.err.println("Неверная команда в запросе!");
                }
            } catch (InterruptedException e) {
                System.err.println("Исключение прерывания!");
            } finally {
                bufferedreader.close();
            }
        } catch (IOException e) {
            System.err.println("Неверный запрос к системе!");
        }
        System.out.println(l.toString());
    }

    // Пример работы с классами com.sun.jna.*
    // https://github.com/java-native-access/jna
    public static void jnaNativeDemo() {
        var c = Native.load("c", CStdLib.class); // /lib/x86_64-linux-gnu/libc.so 
        String msg = "Всем привет!\nЭто пример прямого системного вызова ядра Linux!\n";
        int sys_write = 1, handle_stdout = 1, number_bytes = (msg.getBytes(StandardCharsets.UTF_8).length);
        c.syscall(sys_write, handle_stdout, msg, number_bytes); // Пример системного вызова syscall

    }

    // Пример работы с классами jdk.incubator.foreign
    // https://jdk.java.net/panama/17/
    public static void incubatorForeignDemo() {
        System.loadLibrary("hello");
        var libraryLookup = SymbolLookup.loaderLookup().lookup("printHello");
        if (libraryLookup.isPresent()) {
            var memoryAddress = libraryLookup.get();
            var functionDescriptor = FunctionDescriptor.ofVoid();
            var methodType = MethodType.methodType(Void.TYPE);
            var methodHandle = CLinker.getInstance().downcallHandle(
                    memoryAddress, methodType, functionDescriptor);
            try {
                methodHandle.invokeExact();
            } catch (Throwable ex) {
                System.err.println("Error invokeExact!");
            }
        }
    }
}
