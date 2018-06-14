import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import utils.FileUtil;
import utils.IMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Application implements IMessage {

    private Application() {}

    public static void main(String... args) throws IOException {
        check(args);

        final String filePath;
        Set<String> compilerArgs = new HashSet<>();

        // loading args
        if(args.length==1)
            filePath = args[0];
        else{
            compilerArgs =  buildArgSet(args);
            filePath = args[args.length-1];
        }

        // loading files
        final InputStream inputStream = FileUtil.read(filePath);
        final ANTLRInputStream input = new ANTLRInputStream(inputStream);

        // running lexer
        final SmergielLexer lexer = new SmergielLexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);

        //running parser
        final SmergielParser parser = new SmergielParser(tokens);
        final SmergielParser.ProgramContext program = parser.program();

        //semantics and java source generator
        final ParseTreeWalker walker = new ParseTreeWalker();
        final String className = buildClassName(filePath);
        final SmergielJavaListener listener = new SmergielJavaListener(className);
        walker.walk(listener, program);

        //building class
        final ClassBuilder microClass = new ClassBuilder(className, buildFilePath(filePath), listener.getMain());

        //builds jar file
        if(compilerArgs.contains("jar")) {
            System.out.println(BUILDING_JAR);
            microClass.compileJar();
        }

        System.out.println(COMPILATION_OK);

        //runs smergiel program
        if (compilerArgs.contains("run")) {
            System.out.println(RUNNING);
            microClass.run();
        }


    }

    //checks main args
    private static void check(String... args){
        if(args.length==0) {
            System.err.println(FILE_PATH_ERROR);
            System.exit(NOT_PERMITTED);
        }

        final String filePath;
        if(args.length==1)
            filePath = args[0];
        else
            filePath = args[args.length-1];

        if(!filePath.endsWith(".smr")) {
            System.err.println(FILE_NAME_ERROR);
            System.exit(INVALID_ARGUMENT);
        }

    }

    //builds the class name. Example: test.smr -> Test
    public static String buildClassName(String originPath){
        final String name = originPath.replace(".smr","");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    //builds the target file path without the file name. Example: /tmp/test.smr -> /tmp
    public static String buildFilePath(String originPath){
        final int length = (buildClassName(originPath) + ".smr").length();
        final String path = originPath.substring(0, originPath.length() - length);
        return path.isEmpty() ? "" : path+"/";
    }

    //builds a set witch contains all the arguments but the last, because that's the file path
    public static Set<String> buildArgSet(String... args){
        final List<String> list = Arrays.asList(args).subList(0, args.length-1);
        return new HashSet<>(list);
    }

}
