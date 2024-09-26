package online.flowerinsnow.miteoperator;

import online.flowerinsnow.miteoperator.util.TransformUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class MITEOperatorAgent implements ClassFileTransformer {
    public static void premain(String agentArgs, Instrumentation inst) {
        MITEOperatorAgent agent = new MITEOperatorAgent();
        inst.addTransformer(agent);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        switch (className) {
            case "aa": // CommandHandler
                return this.transformCommandHandler(classfileBuffer);
            case "am": // CommandGive 还原被禁用的方法
                return this.getBytesFromResources("/class_am");
            case "id": // CommandServerOp 还原被禁用的方法
                return this.getBytesFromResources("/class_id");
            case "io": // CommandServerTp 还原被禁用的方法
                return this.transformCommandServerTp(this.getBytesFromResources("/class_io"));
            case "ak": // CommandGameMode 还原被禁用的方法
                return this.getBytesFromResources("/class_ak");
            case "ka": // NetServerHandler 禁用DELETE键
                return this.transformNetServerHandler(classfileBuffer);
            case "hn": // ServerConfigurationManager Necessary
                return this.transformServerConfigurationManager(classfileBuffer);
            case "ir": // DedicatedPlayerList 还原原版权限机制
                return this.transformDedicatedPlayerList(classfileBuffer);
            case "bcw": // 正版验证类 改为使用 HTTPS
                return this.transformBCW(classfileBuffer);
            case "is": // DedicatedServer 还原原版权限机制
                return this.transformDedicatedServer(classfileBuffer);
            case "atv": // Minecraft 将 inDevMode() 返回 true
                return this.transformATV(classfileBuffer);
            case "jv": // EntityPlayerMP 还原原版权限机制
                return this.transformEntityPlayerMP(classfileBuffer);
            case "z": // CommandBase 还原原版权限机制
                return this.transformCommandBase(classfileBuffer);
            default:
                return classfileBuffer;
        }
    }

    private byte[] transformATV(byte[] bytes) {
        return TransformUtils.transformClass(bytes, cn -> {
            TransformUtils.transformMethod(cn, (mn, actions) -> {
                if ("inDevMode".equals(mn.name) && "()Z".equals(mn.desc)) {
                    mn.instructions.clear();
                    mn.instructions.add(new InsnNode(Opcodes.ICONST_1));
                    mn.instructions.add(new InsnNode(Opcodes.IRETURN));
                }
            });
        });
    }

    private byte[] transformCommandHandler(byte[] bytes) {
        return TransformUtils.transformClass(bytes, cn -> {
                    {
                        MethodNode mn = new MethodNode();
                        mn.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
                        mn.name = "isPlayerHasPermission";
                        mn.desc = "(Ljava/lang/String;)Z";
                        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/server/MinecraftServer", "F", "()Lnet/minecraft/server/MinecraftServer;"));
                        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/server/MinecraftServer", "af", "()Lhn;"));
                        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "hn", "e", "(Ljava/lang/String;)Z"));
                        mn.instructions.add(new InsnNode(Opcodes.IRETURN));
                        cn.methods.add(mn);
                    }
            TransformUtils.transformMethod(cn, (mn, lazyActions) -> {
                if ("executeCommand".equals(mn.name) && "(Lad;Ljava/lang/String;Z)I".equals(mn.desc)) {
                    InsnList instructions = mn.instructions;
                    int index = 0;
                    for (int i = 0; i < instructions.size(); i++) {
                        AbstractInsnNode abstractInsnNode = instructions.get(i);
                        if (abstractInsnNode.getOpcode() == Opcodes.ALOAD) {
                            VarInsnNode node = (VarInsnNode) abstractInsnNode;
                            if (node.var == 10 && ++index == 33) {
                                lazyActions.add(() -> {
                                    instructions.insertBefore(node, new LdcInsnNode(0));
                                    instructions.insertBefore(node, new VarInsnNode(Opcodes.ISTORE, 13));
                                    instructions.insertBefore(node, new LabelNode());
                                });
                                break;
                            }
                        }
                    }

                    // 允许op使用/decoy等命令
                    for (int i = 0; i < instructions.size(); i++) {
                        AbstractInsnNode abstractInsnNode = instructions.get(i);
                        if (abstractInsnNode instanceof MethodInsnNode) {
                            MethodInsnNode node = (MethodInsnNode) abstractInsnNode;
                            if ("aa".equals(node.owner) && "isUserPrivileged".equals(node.name) && "(Luf;)Z".equals(node.desc)) {
                                lazyActions.add(() -> {
                                    AbstractInsnNode temp = node.getPrevious().getPrevious();
                                    instructions.remove(temp); // 删除 this
                                    instructions.insertBefore(node, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "jv", "an", "()Ljava/lang/String;")); // getEntityName()
                                    // 改为调用 CommandHandler.isPlayerHasPermission(String)
                                    instructions.insertBefore(node, new MethodInsnNode(Opcodes.INVOKESTATIC, "aa", "isPlayerHasPermission", "(Ljava/lang/String;)Z"));
                                    instructions.remove(node);
                                });
                                break;
                            }
                        }
                    }

                    // 允许op使用/level等命令
                    index = 0;
                    for (int i = 0; i < instructions.size(); i++) {
                        AbstractInsnNode abstractInsnNode = instructions.get(i);
                        if (abstractInsnNode.getOpcode() == Opcodes.INVOKESTATIC) {
                            MethodInsnNode node = (MethodInsnNode) abstractInsnNode;
                            if ("atv".equals(node.owner) && "inDevMode".equals(node.name) && "()Z".equals(node.desc)) {
                                ++index;
                                if (index == 6) {
                                    // 后台拥有所有权限，去除Dev判断
                                    lazyActions.add(() -> {
                                        instructions.remove(node.getNext()); // 去除IFEQ
                                        instructions.remove(node);
                                    });
                                } else if (index == 8) {
                                    lazyActions.add(() -> {
                                        instructions.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 6)); // player
                                        instructions.insertBefore(node, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "jv", "an", "()Ljava/lang/String;")); // getEntityName()
                                        instructions.insertBefore(node, new MethodInsnNode(Opcodes.INVOKESTATIC, "aa", "isPlayerHasPermission", "(Ljava/lang/String;)Z"));
                                        instructions.remove(node);
                                    });
                                }
                            }
                        }
                    }
                }
            });
        });
    }

    // 禁用DELETE键
    private byte[] transformNetServerHandler(byte[] bytes) {
        return TransformUtils.transformClass(bytes, cn -> {
            TransformUtils.transformMethod(cn, (mn, actions) -> {
                if ("handleSimpleSignal".equals(mn.name) && "(LPacket85SimpleSignal;)V".equals(mn.desc)) {
                    InsnList instructions = mn.instructions;

                    int index = 0;
                    for (AbstractInsnNode insnNode : instructions) {
                        if (insnNode.getOpcode() == Opcodes.ALOAD) {
                            VarInsnNode node = (VarInsnNode) insnNode;
                            if (node.var == 0 && index++ == 72) {
                                actions.add(() -> {
                                    instructions.insertBefore(node, new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/server/MinecraftServer", "setTreacheryDetected", "()V", false));
                                });
                                break;
                            }
                        }
                    }
                }
            });
        });
    }

    private byte[] transformCommandServerTp(byte[] bytes) {
        return TransformUtils.transformClassWithoutComputeFrames(bytes, cn -> {
            TransformUtils.transformMethod(cn, (mn, lazyAction) -> {
                if ("b".equals(mn.name) && "(Lad;[Ljava/lang/String;)V".equals(mn.desc)) {
                    InsnList instructions = mn.instructions;

                    // 由于 CommandBase 中修改了解析参数方法，需要将func_110666_a(ICommandSender, double, String) 重定向到 func_110666_a(ICommandSender, double, String, int, int)
                    // 并加上最大数值的Ldc
                    for (AbstractInsnNode insnNode : instructions) {
                        if (insnNode.getOpcode() == Opcodes.INVOKESTATIC) {
                            MethodInsnNode node = (MethodInsnNode) insnNode;
                            if ("io".equals(node.owner) && "a".equals(node.name) && "(Lad;DLjava/lang/String;)D".equals(node.desc)) {
                                lazyAction.add(() -> {
                                    instructions.insertBefore(node, new LdcInsnNode(-30000000));
                                    instructions.insertBefore(node, new LdcInsnNode(30000000));
                                    node.name = "func_110666_a";
                                    node.desc = "(Lad;DLjava/lang/String;II)D";
                                });
                            }
                        }
                    }
                }
            });
        });
    }

    private byte[] transformServerConfigurationManager(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        // 从原版端还原 loadOpsList() 和 saveOpsList()
        cr = new ClassReader(this.getBytesFromResources("/class_hn"));
        ClassNode vanillaCn = new ClassNode();
        cr.accept(vanillaCn, 0);

        // 还原 addOp(String) 方法，它在 MITE 中被清空了方法体
        // 还原 isPlayerOpped(String) 方法，它在 MITE 中直接返回了 false
        cn.methods.removeIf(mn ->
                ("b".equals(mn.name) && "(Ljava/lang/String;)V".equals(mn.desc)) ||
                        ("e".equals(mn.name) && "(Ljava/lang/String;)Z".equals(mn.desc))
        );
        vanillaCn.methods.forEach(mn -> {
            if (("b".equals(mn.name) && "(Ljava/lang/String;)V".equals(mn.desc)) ||
                    ("e".equals(mn.name) && "(Ljava/lang/String;)Z".equals(mn.desc))) {
                cn.methods.add(mn);
            }
        });

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private byte[] transformDedicatedPlayerList(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        // 从原版端还原 loadOpsList() 方法，MITE 将这个方法直接设为清空 op 列表
        // 还原 saveOpsList() 还原，MITE 将这个方法体直接清空
        cr = new ClassReader(this.getBytesFromResources("/class_ir"));
        ClassNode vanillaCn = new ClassNode();
        cr.accept(vanillaCn, 0);

        cn.methods.removeIf(mn ->
                ("u".equals(mn.name) && "()V".equals(mn.desc)) ||
                        ("t".equals(mn.name) && "()V".equals(mn.desc))
        );
        vanillaCn.methods.forEach(mn -> {
            if (("u".equals(mn.name) && "()V".equals(mn.desc)) ||
                    ("t".equals(mn.name) && "()V".equals(mn.desc))) {
                cn.methods.add(mn);
            }
        });

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private byte[] transformDedicatedServer(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        // 从原版端还原 func_110455_j() （推测应该是 getOpPermissionLevel）
        // MITE 将其直接清空并返回了 0
        cr = new ClassReader(this.getBytesFromResources("/class_is"));
        ClassNode vanillaCn = new ClassNode();
        cr.accept(vanillaCn, 0);

        cn.methods.removeIf(mn ->
                ("k".equals(mn.name) && "()I".equals(mn.desc))
        );
        vanillaCn.methods.forEach(mn -> {
            if ("k".equals(mn.name) && "()I".equals(mn.desc)) {
                cn.methods.add(mn);
            }
        });

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private byte[] transformEntityPlayerMP(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        // 从原版端还原 canCommandSenderUseCommand(int, String)
        // 禁用了 MITE 的 inDevMode() 判断
        cr = new ClassReader(this.getBytesFromResources("/class_jv"));
        ClassNode vanillaCn = new ClassNode();
        cr.accept(vanillaCn, 0);

        cn.methods.removeIf(mn ->
                ("a".equals(mn.name) && "(ILjava/lang/String;)Z".equals(mn.desc))
        );
        vanillaCn.methods.forEach(mn -> {
            if ("a".equals(mn.name) && "(ILjava/lang/String;)Z".equals(mn.desc)) {
                cn.methods.add(mn);
            }
        });

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private byte[] transformBCW(byte[] bytes) {
        return TransformUtils.transformClass(bytes, cn -> {
            TransformUtils.transformMethod(cn, (mn, lazyActions) -> {
                if ("a".equals(mn.name) && "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;".equals(mn.desc)) {
                    InsnList instructions = mn.instructions;
                    for (int i = 0; i < instructions.size(); i++) {
                        AbstractInsnNode abstractInsnNode = instructions.get(i);
                        if (abstractInsnNode.getOpcode() == Opcodes.LDC) {
                            LdcInsnNode node = (LdcInsnNode) abstractInsnNode;
                            if ("http://session.minecraft.net/game/joinserver.jsp?user=".equals(node.cst)) {
                                node.cst = "https://session.minecraft.net/game/joinserver.jsp?user=";
                            }
                        }
                    }
                }
            });
        });
    }

    private byte[] transformCommandBase(byte[] bytes) {
        return TransformUtils.transformClass(bytes, cn -> {
            TransformUtils.transformMethod(cn, (mn, lazyActions) -> {
                // static notifyAdmins(ICommandSender, int, String, Object...)
                if ("a".equals(mn.name) && "(Lad;ILjava/lang/String;[Ljava/lang/Object;)V".equals(mn.desc)) {
                    InsnList instructions = mn.instructions;
                    for (int i = 0; i < instructions.size(); i++) {
                        AbstractInsnNode abstractInsnNode = instructions.get(i);
                        if (abstractInsnNode instanceof JumpInsnNode) {
                            JumpInsnNode node = (JumpInsnNode) abstractInsnNode;
                            if (node.getOpcode() == Opcodes.IFNULL) {
                                // par0ICommandSender instanceof DedicatedServer
                                InsnList list = new InsnList();
                                list.add(new LabelNode());
                                list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                list.add(new TypeInsnNode(Opcodes.INSTANCEOF, "is"));
                                list.add(new JumpInsnNode(Opcodes.IFNE, node.label));
                                lazyActions.add(() -> {
                                    instructions.insert(node, list);
                                });
                            }
                        }
                    }
                }
            });
        });
    }

    private byte[] getBytesFromResources(String path) {
        try (InputStream in = MITEOperatorAgent.class.getResourceAsStream(path)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read;
            byte[] bytes = new byte[1024];
            //noinspection DataFlowIssue
            while ((read = in.read(bytes)) != -1) {
                baos.write(bytes, 0, read);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
