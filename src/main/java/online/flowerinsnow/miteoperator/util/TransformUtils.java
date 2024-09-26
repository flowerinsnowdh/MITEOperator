package online.flowerinsnow.miteoperator.util;

import online.flowerinsnow.miteoperator.exception.NoSuchMethodNodeException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class TransformUtils {
    private TransformUtils() {
    }

    public static byte[] transformClass(byte[] classBuf, Consumer<ClassNode> action) {
        ClassReader cr = new ClassReader(classBuf);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_FRAMES);
        action.accept(cn);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    public static byte[] transformClassWithoutComputeFrames(byte[] classBuf, Consumer<ClassNode> action) {
        ClassReader cr = new ClassReader(classBuf);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        action.accept(cn);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    public static void transformMethod(ClassNode cn, BiConsumer<MethodNode, List<Runnable>> action) {
        List<Runnable> lazyActions = new ArrayList<>();
        cn.methods.forEach(mn -> action.accept(mn, lazyActions));
        lazyActions.forEach(Runnable::run);
    }

    public static void transformMethod(ClassNode cn, String name, String desc, BiConsumer<MethodNode, List<Runnable>> action) {
        List<Runnable> lazyActions = new ArrayList<>();
        action.accept(cn.methods.stream()
                .filter(mn -> mn.name.equals(name) && mn.desc.equals(desc))
                .findAny()
                .orElseThrow(() -> new NoSuchMethodNodeException(name + desc))
                , lazyActions);
        lazyActions.forEach(Runnable::run);
    }
}
