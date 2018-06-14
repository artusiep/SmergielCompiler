package utils;

public interface IMessage {
    String COMPILER_INFO = "Smergiel Info: ";
    String COMPILER_ERROR = "Smergiel Error: ";

    String FILE_PATH_ERROR = COMPILER_ERROR + "File path is needed to proceed";
    String PARAM_NUMBER_ERROR = COMPILER_ERROR + "Wrong number of parameters";
    String FILE_NAME_ERROR = COMPILER_ERROR + "File name must ent with '.m'";
    String ID_NOT_DECLARED_ERROR = COMPILER_ERROR + "Line %s:%s - Identifier \"%s\" is not declared\n";
    String IO_ERROR = COMPILER_ERROR + "I/O Error or file not found: \"%s\"";
    String COMPILATION_OK = COMPILER_INFO + "COMPILATION SUCCESSFUL";
    String BUILDING_JAR = COMPILER_INFO + "BUILDING JAR ";
    String RUNNING = COMPILER_INFO + "SUCCED \n ----------- ";

    int INVALID_ARGUMENT = 22;
    int ARG_LIST_TOO_LONG = 7;
    int NOT_PERMITTED = 7;
}
