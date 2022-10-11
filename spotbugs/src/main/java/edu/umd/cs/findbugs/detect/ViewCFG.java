package edu.umd.cs.findbugs.detect;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Edge;
import static edu.umd.cs.findbugs.ba.EdgeTypes.*;

public class ViewCFG implements Detector {

    private final Pattern SPACE_ARROW = Pattern.compile(" ->");
    private final Pattern NUMBER_SUFFIX = Pattern.compile(" (\\d+)$");
    private final Pattern SPECIAL_METHOD = Pattern.compile("<(\\w+)>");

    private final BugReporter bugReporter;
    private Path tempDir;

    public ViewCFG(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        try {
            tempDir = Files.createTempDirectory("cfg-");
        } catch (IOException e) {
            bugReporter.logError("Could not create temporary directory", e);
        }
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (tempDir == null) {
            return;
        }

        JavaClass cls = classContext.getJavaClass();
        String classDirName = (!cls.getPackageName().isEmpty()) ? (cls.getPackageName() + "." + cls.getClassName()) : cls.getClassName();
        Path classDir;

        try {
            classDir = Files.createDirectory(Paths.get(tempDir.toString(), classDirName));
        } catch (IOException e) {
            bugReporter.logError("Could not create directory for class " + cls.getClassName(), e);
            return;
        }

        for (Method method : cls.getMethods()) {
            try {
                analyzeMethod(classContext, method, classDir);
            } catch (CFGBuilderException e) {
                bugReporter.logError("Error analyzing method", e);
            }
        }
    }

    private void analyzeMethod(ClassContext classContext, Method method, Path classDir) throws CFGBuilderException {
        Path methodFile = getMethodFile(classDir, method.getName());
        PrintStream out;

        try {
            out = new PrintStream(Files.createFile(methodFile).toFile(), Charset.defaultCharset().name());
        } catch (IOException e) {
            bugReporter.logError("Could not create file for method " + method.getName(), e);
            return;
        }

        CFG cfg = classContext.getCFG(method);
        out.println("digraph " + method.getName() + " {");
        for (Iterator<BasicBlock> bi = cfg.blockIterator(); bi.hasNext();) {
            BasicBlock block = bi.next();
            if (block == cfg.getEntry()) {
                out.println("  Node" + block.getLabel() + " [shape=record label=\"{" + block.getLabel() +
                        " (ENTRY) }\"];");
                continue;
            }

            if (block == cfg.getExit()) {
                out.println("  Node" + block.getLabel() + " [shape=record label=\"{" + block.getLabel() +
                        " (EXIT) }\"];");
                continue;
            }

            out.print("  Node" + block.getLabel() + " [shape=record label=\"{" + block.getLabel());
            if (block.getFirstInstruction() != null) {
                out.print("|");
            }
            for (Iterator<InstructionHandle> ii = block.instructionIterator(); ii.hasNext();) {
                InstructionHandle ins = ii.next();
                String insStr = NUMBER_SUFFIX.matcher(
                        SPACE_ARROW.matcher(ins.toString(false)).replaceAll(""))
                        .replaceAll(" #$1");
                out.print(insStr + "\\l");
            }
            out.println("}\"];");
        }

        for (Iterator<Edge> ei = cfg.edgeIterator(); ei.hasNext();) {
            Edge edge = ei.next();
            BasicBlock src = edge.getSource();
            BasicBlock tgt = edge.getTarget();
            switch (edge.getType()) {
            default:
                out.println("  Node" + src.getLabel() + " -> Node" + tgt.getLabel() + ";");
                break;
            case IFCMP_EDGE:
                out.println("  Node" + src.getLabel() + " -> Node" + tgt.getLabel() +
                        " [shape=plaintext label=\" True branch\"];");
                break;
            case HANDLED_EXCEPTION_EDGE:
                out.println("  Node" + src.getLabel() + " -> Node" + tgt.getLabel() +
                        " [shape=plaintext label=\" Handled exception for #" +
                        edge.getSource().getExceptionThrower().getPosition() + "\"];");
                break;
            case UNHANDLED_EXCEPTION_EDGE:
                out.println("  Node" + src.getLabel() + " -> Node" + tgt.getLabel() +
                        " [shape=plaintext label=\" Unhandled exception for #" +
                        edge.getSource().getExceptionThrower().getPosition() + "\"];");
                break;
            case RETURN_EDGE:
                out.println("  Node" + src.getLabel() + " -> Node" + tgt.getLabel() +
                        " [shape=plaintext label=\" Return\"];");
                break;
            case START_EDGE:
                out.println("  Node" + src.getLabel() + " -> Node" + tgt.getLabel() +
                        " [shape=plaintext label=\" Start\"];");
                break;
            case EXIT_EDGE:
                out.println("  Node" + src.getLabel() + " -> Exit" + tgt.getLabel() +
                        " [shape=plaintext label=\" Exit\"];");
                break;
            case SWITCH_EDGE:
                out.println("  Node" + src.getLabel() + " -> Node" + tgt.getLabel() +
                        " [shape=plaintext label=\" Switch case (non-default)\"];");
                break;
            case SWITCH_DEFAULT_EDGE:
                out.println("  Node" + src.getLabel() + " -> Node" + tgt.getLabel() +
                        " [shape=plaintext label=\" Switch case (default)\"];");
                break;
            case JSR_EDGE:
                out.println("  Node" + src.getLabel() + " -> Node" + tgt.getLabel() +
                        " [shape=plaintext label=\" JSR statement\"];");
                break;
            case RET_EDGE:
                out.println("  Node" + src.getLabel() + " -> Node" + tgt.getLabel() +
                        " [shape=plaintext label=\" RET statement\"];");
                break;
            case GOTO_EDGE:
                out.println("  Node" + src.getLabel() + " -> Node" + tgt.getLabel() +
                        " [shape=plaintext label=\" GOTO statement\"];");
                break;
            }
        }
        out.println("}");
        out.close();
        if (out.checkError()) {
            bugReporter.logError("Error writing to file " + methodFile.toString());
        }
    }

    private Path getMethodFile(Path classDir, String methodName) {
        String methodFileNameBase = SPECIAL_METHOD.matcher(methodName).replaceAll("____$1");
        Path methodFile;
        int index = 0;

        String methodFileName = methodFileNameBase;
        do {
            methodFile = Paths.get(classDir.toString(), methodFileName + ".dot");
            methodFileName = methodFileNameBase + ++index;
        } while (Files.exists(methodFile));
        return methodFile;
    }

    @Override
    public void report() {
        if (tempDir != null) {
            System.out.println("CFGs generated into directory: " + tempDir + ". Please do not forget to delete it.");
        }
    }
}
