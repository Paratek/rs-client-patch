package io.paratek.patch;

import com.google.common.flogger.FluentLogger;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ListIterator;


/**
 * How it works:
 *
 * 1. Make a backup
 * 2. Read the default jagexappletviewer.jar
 * 3. Set a couple fields in the appletviewer class so that other exceptions won't be thrown
 * 4. Write back out
 * 5. Profi... nah i'm still poor
 */
public class Application {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private static final String APPLET = "jagexappletviewer.jar";
    private static String VIEWER_PATH;

    static {
        // Idk the registry key tbh
        final String homePath = System.getProperty("user.home");
        if (System.getProperty("os.name").contains("win")) {
            if (new File(homePath + "\\jagexcache\\jagexlauncher\\bin").exists()) {
                VIEWER_PATH = homePath + "\\jagexcache\\jagexlauncher\\bin\\";
            } else {
                VIEWER_PATH = homePath + "\\OneDrive\\jagexcache\\jagexlauncher\\bin\\";
            }
        }
    }

    public static void main(String[] args) {
        logger.atInfo().log("Jar Path -> " + VIEWER_PATH);
        if (new File(VIEWER_PATH + "jagexappletviewer_original.jar").exists()) {
            logger.atInfo().log("Patch is already applied");
            System.exit(42069);
        }

        final File original = new File(VIEWER_PATH + APPLET);
        if (!original.exists()) {
            logger.atSevere().log("jagexappletviewer.jar does not exist, please install RuneScape or OldSchool RuneScape");
            System.exit(69);
        }
        // Backup the current jar1
        try {
            Files.copy(new File(VIEWER_PATH + APPLET).toPath(), new File(VIEWER_PATH + "jagexappletviewer_original.jar").toPath());
        } catch (IOException e) {
            logger.atSevere().log("Unable to create a backup jagexappletviewer.jar. May be a permissions error, or perhaps the file does not exist?");
            System.exit(420);
        }
        // Delete the original
        logger.atInfo().log("Deleting the original " + original.delete());

        // Load the jar
        final JarHandler handler = new JarHandler(VIEWER_PATH + "jagexappletviewer_original.jar");

        // remove the System.load call and replace it with a sysout so no instruction need to be cleaned up
        final ClassNode appletViewer = handler.getClassMap().get("app.appletviewer");
        appletViewer.methods.forEach(methodNode -> {
            for (ListIterator<AbstractInsnNode> it = methodNode.instructions.iterator(); it.hasNext(); ) {
                AbstractInsnNode node = it.next();
                if (node instanceof MethodInsnNode && ((MethodInsnNode) node).owner.equals("java/lang/System")
                        && ((MethodInsnNode) node).name.equals("load")) {
                    it.previous();
                    it.previous();
                    it.previous();
                    it.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                    it.next();
                    it.next();
                    it.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
                    it.next();
                    it.remove();
                }
            }
        });

        // Dump
        handler.dumpTo(VIEWER_PATH + APPLET);
        logger.atInfo().log("Patch Successful!");
    }

}
