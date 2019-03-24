package io.paratek.patch;

import com.google.common.flogger.FluentLogger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
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
        if (new File(homePath + "\\jagexcache\\jagexlauncher\\bin").exists()) {
            VIEWER_PATH = homePath + "\\jagexcache\\jagexlauncher\\bin\\";
        } else {
            VIEWER_PATH = homePath + "\\OneDrive\\jagexcache\\jagexlauncher\\bin\\";
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

        // prune appletviewer
        // This is no longer a supported client, I doubt this will ever be updated so there is no need to hook appletviewer.l or appletviewer.b
        // appletviewer.l is true if os.name is Windows
        // appletviewer.b is true if x64 or i686? I think, I forgot tho.
        final ClassNode appletViewer = handler.getClassMap().get("app.appletviewer");
        for (MethodNode methodNode : (List<MethodNode>) appletViewer.methods) {
            final ListIterator<AbstractInsnNode> nodeListIterator = methodNode.instructions.iterator();
            while (nodeListIterator.hasNext()) {
                final AbstractInsnNode curr = nodeListIterator.next();
                if (curr instanceof FieldInsnNode && ((FieldInsnNode) curr).owner.contains("appletviewer")
                        && ((FieldInsnNode) curr).name.equals("l") && ((FieldInsnNode) curr).desc.equals("Z")
                        && curr.getOpcode() == Opcodes.PUTSTATIC) {
                    nodeListIterator.add(new InsnNode(Opcodes.ICONST_0));
                    nodeListIterator.add(new FieldInsnNode(Opcodes.PUTSTATIC, ((FieldInsnNode) curr).owner, ((FieldInsnNode) curr).name, ((FieldInsnNode) curr).desc));
                } else if (curr instanceof FieldInsnNode && ((FieldInsnNode) curr).owner.contains("appletviewer")
                        && ((FieldInsnNode) curr).name.equals("b") && ((FieldInsnNode) curr).desc.equals("Z")
                        && curr.getOpcode() == Opcodes.PUTSTATIC) {
                    nodeListIterator.add(new InsnNode(Opcodes.ICONST_1));
                    nodeListIterator.add(new FieldInsnNode(Opcodes.PUTSTATIC, ((FieldInsnNode) curr).owner, ((FieldInsnNode) curr).name, ((FieldInsnNode) curr).desc));
                }
            }
        }

        // Dump
        handler.dumpTo(VIEWER_PATH + APPLET);
        logger.atInfo().log("Patch Successful!");
    }

}
