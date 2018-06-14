import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.IMessage;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class ClassBuilder implements IMessage {

    private static final String TEMP_PATH = System.getProperty("java.io.tmpdir");

    private final String sourceCode;
    private final String name;
    private String target = "";


    public ClassBuilder(@NotNull String name, @Nullable String target, @NotNull String sourceCode) {
        this.sourceCode = sourceCode;
        this.name = name;
        this.target = target== null ? TEMP_PATH : target;
    }

    public StandardJavaFileManager compile(){
        final JavaFileObject source = new JavaSourceFromString(name, sourceCode);
        final Iterable<? extends JavaFileObject> files = Collections.singletonList(source);
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final StandardJavaFileManager mgr = compiler.getStandardFileManager(null, null, null);
        try {
            mgr.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(new File(TEMP_PATH)));
        } catch (final IOException e) {
            System.err.printf(IO_ERROR, target+name);
            System.exit(1);
        }
        compiler.getTask(null, mgr, null, null, null, files).call();

        return mgr;


    }

    public void run(){
        try {
            final StandardJavaFileManager mgr = compile();
            final ClassLoader cl = mgr.getClassLoader(StandardLocation.CLASS_OUTPUT);
            final Class<?> clazz = Class.forName(name, true, cl);
            final Method method = clazz.getMethod("main", String[].class);

            final String[] params = null;
            method.invoke(null, (Object) params);

        }catch (final ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e){
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public void compileJar(){
        compile();
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Main-Class"), ""+name);

        try(FileOutputStream fout= new FileOutputStream(target+name+".jar"); JarOutputStream jarOut = new JarOutputStream(fout, manifest)) {
            final String classFile = name + ".class";

            jarOut.putNextEntry(new ZipEntry(""+classFile));
            jarOut.write(Files.readAllBytes(Paths.get(TEMP_PATH +"/"+ classFile)));

            jarOut.closeEntry();
            jarOut.close();
            fout.close();
        }catch(final IOException e){
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }


    class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///"+name+ Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

}
