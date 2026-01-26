package cn.flowerinsnow.miteoperator;

import cn.flowerinsnow.miteoperator.config.AgentConfig;
import cn.flowerinsnow.miteoperator.util.TransformUtils;
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
        AgentConfig.load();
        MITEOperatorAgent agent = new MITEOperatorAgent();
        inst.addTransformer(agent);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        switch (className) {
            case "aa": // CommandHandler
                return this.transformCommandHandler(classfileBuffer);
            case "am": // CommandGive 还原被禁用的命令
                return this.getBytesFromResources("classes/am.class");
            case "id": // CommandServerOp 还原被禁用的命令
                return this.getBytesFromResources("classes/id.class");
            case "io": // CommandServerTp 还原被禁用的命令
                return this.transformCommandServerTp(this.getBytesFromResources("classes/io.class"));
            case "ak": // CommandGameMode 还原被禁用的命令
                return this.getBytesFromResources("classes/ak.class");
            case "ah": // CommandEnchant 还原被禁用的命令
                return this.transformCommandEnchant(this.getBytesFromResources("classes/ah.class"));
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
            case "ud": // InventoryPlayer 防止死亡掉落
                return this.transformInventoryPlayer(classfileBuffer);
            case "z": // CommandBase 还原原版权限机制
                return this.transformCommandBase(classfileBuffer);
            default:
                if (looksLikeEntityPlayerMP(classfileBuffer)) {
                    return this.transformEntityPlayerMPDeathOnly(classfileBuffer);
                }
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
            TransformUtils.transformMethod(cn, (mn, lazyActions) -> {
                if ("handleSimpleSignal".equals(mn.name) && "(LPacket85SimpleSignal;)V".equals(mn.desc)) {
                    InsnList instructions = mn.instructions;

                    int index = 0;
                    for (AbstractInsnNode insnNode : instructions) {
                        if (insnNode.getOpcode() == Opcodes.ALOAD) {
                            VarInsnNode node = (VarInsnNode) insnNode;
                            if (node.var == 4 && index++ == 31) {
                                lazyActions.add(() -> {
                                    /*
                                    + this.kickPlayerFromServer("Treachery detected!");
                                     */
                                    instructions.insertBefore(insnNode, new VarInsnNode(Opcodes.ALOAD, 0));
                                    instructions.insertBefore(insnNode, new LdcInsnNode("Treachery detected!"));
                                    instructions.insertBefore(insnNode, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "ka", "c", "(Ljava/lang/String;)V"));
                                    for (int i = 0; i < 18; i++) {
                                        AbstractInsnNode next = insnNode.getNext();
                                        if (next instanceof LabelNode || next instanceof LineNumberNode) {
                                            i--;
                                        }
                                        instructions.remove(next);
                                    }
                                    instructions.remove(insnNode);
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

    private byte[] transformCommandEnchant(byte[] bytes) {
        return TransformUtils.transformClassWithoutComputeFrames(bytes, cn -> {
            TransformUtils.transformMethod(cn, (mn, lazyAction) -> {
                if ("b".equals(mn.name) && "(Lad;[Ljava/lang/String;)V".equals(mn.desc)) { // void processCommand(ICommandSender, String[])
                    InsnList instructions = mn.instructions;

                    // 由于 EntityPlayer 移除了 ItemStack getCurrentEquippedItem()，需要调整为先获取 inventory 再 getCurrentItem()
                    for (AbstractInsnNode insnNode : instructions) {
                        if (insnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                            MethodInsnNode node = (MethodInsnNode) insnNode;
                            if ("uf".equals(node.owner) && "by".equals(node.name) && "()Lye;".equals(node.desc)) {
                                lazyAction.add(() -> {
                                    //   ItemStack var6 = var3.
                                    // - getCurrentEquippedItem()
                                    // + inventory
                                    // + .getCurrentItemStack()
                                    //   ;
                                    instructions.insertBefore(node, new FieldInsnNode(Opcodes.GETFIELD, "uf", "bn", "Lud;"));
                                    instructions.insertBefore(node, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "ud", "getCurrentItemStack", "()Lye;"));
                                    instructions.remove(node);
                                });
                            }
                        }
                    }

                    int index = -1;
                    for (AbstractInsnNode insnNode : instructions) {
                        if (insnNode.getOpcode() == Opcodes.ALOAD) {
                            VarInsnNode varInsnNode = (VarInsnNode) insnNode;
                            if (varInsnNode.var == 7) {
                                index++;
                                if (index == 1) {
                                    lazyAction.add(() -> {
                                        /*
                                          else if (!
                                        - var7.canApply(var6)
                                        + true
                                          )
                                         */
                                        instructions.insertBefore(varInsnNode, new InsnNode(Opcodes.ICONST_1));
                                        instructions.remove(varInsnNode.getNext());
                                        instructions.remove(varInsnNode.getNext());
                                        instructions.remove(varInsnNode);
                                    });
                                } else if (index == 2) {
                                    lazyAction.add(() -> {
                                        /*
                                          var5 = parseIntBounded(par1ICommandSender, par2ArrayOfStr[2],
                                        - var7.getMinLevel(),
                                        + 1,
                                          var7.getMaxLevel());
                                         */
                                        instructions.insertBefore(varInsnNode, new InsnNode(Opcodes.ICONST_1));
                                        instructions.remove(varInsnNode.getNext());
                                        instructions.remove(varInsnNode);
                                    });
                                } else if (index == 3) {
                                    lazyAction.add(() -> {
                                        /*
                                          var5 = parseIntBounded(par1ICommandSender, par2ArrayOfStr[2], var7.getMinLevel(),
                                        - var7.getMaxLevel()
                                        + Byte.MAX_VALUE
                                          );
                                         */
                                        instructions.insertBefore(varInsnNode, new IntInsnNode(Opcodes.BIPUSH, Byte.MAX_VALUE));
                                        instructions.remove(varInsnNode.getNext());
                                        instructions.remove(varInsnNode);
                                    });
                                }
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
        cr = new ClassReader(this.getBytesFromResources("classes/hn.class"));
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

        for (MethodNode mn : cn.methods) {
            if (!mn.desc.endsWith(")L" + "jv" + ";")) {
                continue;
            }
            InsnList instructions = mn.instructions;
            for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode node = (MethodInsnNode) insn;
                if (node.getOpcode() != Opcodes.INVOKEVIRTUAL || !"(Luf;Z)V".equals(node.desc)) {
                    continue;
                }
                AbstractInsnNode prev = getPrevRealInsn(node);
                AbstractInsnNode prev2 = prev == null ? null : getPrevRealInsn(prev);
                AbstractInsnNode prev3 = prev2 == null ? null : getPrevRealInsn(prev2);
                if (!(prev instanceof VarInsnNode) || !(prev2 instanceof VarInsnNode) || !(prev3 instanceof VarInsnNode)) {
                    continue;
                }
                VarInsnNode varFlag = (VarInsnNode) prev;
                VarInsnNode varOld = (VarInsnNode) prev2;
                VarInsnNode varNew = (VarInsnNode) prev3;
                if (varNew.getOpcode() != Opcodes.ALOAD || varOld.getOpcode() != Opcodes.ALOAD) {
                    continue;
                }
                InsnList keepInventory = new InsnList();
                LabelNode skipInventory = new LabelNode();
                keepInventory.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
                keepInventory.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isKeepInventoryOnDeath", "()Z"));
                keepInventory.add(new JumpInsnNode(Opcodes.IFEQ, skipInventory));
                keepInventory.add(new VarInsnNode(Opcodes.ALOAD, varNew.var));
                keepInventory.add(new FieldInsnNode(Opcodes.GETFIELD, "uf", "bn", "Lud;"));
                keepInventory.add(new VarInsnNode(Opcodes.ALOAD, varOld.var));
                keepInventory.add(new FieldInsnNode(Opcodes.GETFIELD, "uf", "bn", "Lud;"));
                keepInventory.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "ud", "b", "(Lud;)V"));
                keepInventory.add(skipInventory);
                instructions.insert(node, keepInventory);
                break;
            }

            for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof FieldInsnNode)) {
                    continue;
                }
                FieldInsnNode getField = (FieldInsnNode) insn;
                if (getField.getOpcode() != Opcodes.GETFIELD || !"respawn_experience".equals(getField.name) || !"I".equals(getField.desc)) {
                    continue;
                }
                AbstractInsnNode next = getNextRealInsn(getField);
                if (!(next instanceof FieldInsnNode)) {
                    continue;
                }
                FieldInsnNode putField = (FieldInsnNode) next;
                if (putField.getOpcode() != Opcodes.PUTFIELD || !"I".equals(putField.desc)) {
                    continue;
                }
                AbstractInsnNode prevOld = getPrevRealInsn(getField);
                AbstractInsnNode prevNew = prevOld == null ? null : getPrevRealInsn(prevOld);
                if (!(prevOld instanceof VarInsnNode) || !(prevNew instanceof VarInsnNode)) {
                    continue;
                }
                VarInsnNode oldVar = (VarInsnNode) prevOld;
                VarInsnNode newVar = (VarInsnNode) prevNew;
                if (oldVar.getOpcode() != Opcodes.ALOAD || newVar.getOpcode() != Opcodes.ALOAD) {
                    continue;
                }

                LabelNode skipOverride = new LabelNode();
                InsnList override = new InsnList();
                override.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
                override.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isKeepExperienceOnDeath", "()Z"));
                override.add(new JumpInsnNode(Opcodes.IFEQ, skipOverride));
                override.add(new VarInsnNode(Opcodes.ALOAD, newVar.var));
                override.add(new VarInsnNode(Opcodes.ALOAD, oldVar.var));
                override.add(new FieldInsnNode(Opcodes.GETFIELD, putField.owner, putField.name, "I"));
                override.add(new FieldInsnNode(Opcodes.PUTFIELD, putField.owner, putField.name, "I"));
                override.add(skipOverride);
                instructions.insert(putField, override);
                break;
            }
        }

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
        cr = new ClassReader(this.getBytesFromResources("classes/ir.class"));
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
        cr = new ClassReader(this.getBytesFromResources("classes/is.class"));
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

        for (MethodNode mn : cn.methods) {
            if (mn.desc.startsWith("(Ljava/lang/String;)L") && mn.desc.endsWith(";") && methodAccessesField(mn, "soonest_reconnection_times")) {
                InsnList guard = new InsnList();
                LabelNode continueLabel = new LabelNode();
                guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
                guard.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isReconnectPenaltyEnabled", "()Z"));
                guard.add(new JumpInsnNode(Opcodes.IFNE, continueLabel));
                guard.add(new InsnNode(Opcodes.ACONST_NULL));
                guard.add(new InsnNode(Opcodes.ARETURN));
                guard.add(continueLabel);
                mn.instructions.insert(guard);
                break;
            }
        }

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
        cr = new ClassReader(this.getBytesFromResources("classes/jv.class"));
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

        applyEntityPlayerMPDeathTransforms(cn);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private byte[] transformInventoryPlayer(byte[] bytes) {
        return TransformUtils.transformClass(bytes, cn -> {
            TransformUtils.transformMethod(cn, (mn, actions) -> {
                if ("m".equals(mn.name) && "()V".equals(mn.desc)) {
                    InsnList head = new InsnList();
                    LabelNode continueLabel = new LabelNode();
                    head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
                    head.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isKeepInventoryOnDeath", "()Z"));
                    head.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
                    head.add(new InsnNode(Opcodes.RETURN));
                    head.add(continueLabel);
                    mn.instructions.insert(head);
                }
            });
        });
    }

    private byte[] transformEntityPlayerMPDeathOnly(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        applyEntityPlayerMPDeathTransforms(cn);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private void applyEntityPlayerMPDeathTransforms(ClassNode cn) {
        String entityPlayerSuper = cn.superName;
        String inventoryDesc = findFieldDesc(cn, "inventory");
        FoodStatsField foodStatsField = resolveFoodStatsField(cn, entityPlayerSuper);
        ExperienceField experienceField = resolveExperienceField(cn, entityPlayerSuper);
        String entityPlayerDesc = "(L" + entityPlayerSuper + ";Z)V";

        if (cn.methods.stream().noneMatch(mn -> "clonePlayer".equals(mn.name) && entityPlayerDesc.equals(mn.desc))) {
            MethodNode clonePlayer = new MethodNode(Opcodes.ACC_PUBLIC, "clonePlayer", entityPlayerDesc, null, null);
            InsnList insn = clonePlayer.instructions;
            insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insn.add(new VarInsnNode(Opcodes.ALOAD, 1));
            insn.add(new VarInsnNode(Opcodes.ILOAD, 2));
            insn.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, entityPlayerSuper, "clonePlayer", entityPlayerDesc));

            LabelNode skipInventory = new LabelNode();
            insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
            insn.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isKeepInventoryOnDeath", "()Z"));
            insn.add(new JumpInsnNode(Opcodes.IFEQ, skipInventory));
            if (inventoryDesc != null) {
                String inventoryOwner = inventoryDesc.substring(1, inventoryDesc.length() - 1);
                insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insn.add(new FieldInsnNode(Opcodes.GETFIELD, entityPlayerSuper, "inventory", inventoryDesc));
                insn.add(new VarInsnNode(Opcodes.ALOAD, 1));
                insn.add(new FieldInsnNode(Opcodes.GETFIELD, entityPlayerSuper, "inventory", inventoryDesc));
                insn.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, inventoryOwner, "copyInventory", "(" + inventoryDesc + ")V"));
            }
            insn.add(skipInventory);

            LabelNode skipExperience = new LabelNode();
            insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
            insn.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isKeepExperienceOnDeath", "()Z"));
            insn.add(new JumpInsnNode(Opcodes.IFEQ, skipExperience));
            if (experienceField != null) {
                insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insn.add(new VarInsnNode(Opcodes.ALOAD, 1));
                if (!entityPlayerSuper.equals(experienceField.owner)) {
                    insn.add(new TypeInsnNode(Opcodes.CHECKCAST, experienceField.owner));
                }
                insn.add(new FieldInsnNode(Opcodes.GETFIELD, experienceField.owner, experienceField.name, experienceField.desc));
                insn.add(new FieldInsnNode(Opcodes.PUTFIELD, experienceField.owner, experienceField.name, experienceField.desc));
            }
            insn.add(skipExperience);

            LabelNode skipHunger = new LabelNode();
            insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
            insn.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isKeepHungerOnDeath", "()Z"));
            insn.add(new JumpInsnNode(Opcodes.IFEQ, skipHunger));
            if (foodStatsField != null) {
                insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insn.add(new VarInsnNode(Opcodes.ALOAD, 1));
                insn.add(new FieldInsnNode(Opcodes.GETFIELD, foodStatsField.owner, foodStatsField.name, foodStatsField.desc));
                insn.add(new FieldInsnNode(Opcodes.PUTFIELD, foodStatsField.owner, foodStatsField.name, foodStatsField.desc));
            }
            insn.add(skipHunger);
            insn.add(new InsnNode(Opcodes.RETURN));
            cn.methods.add(clonePlayer);
        }

        MethodNode experienceValue = null;
        for (MethodNode mn : cn.methods) {
            if ("getExperienceValue".equals(mn.name) && "()I".equals(mn.desc)) {
                experienceValue = mn;
                break;
            }
        }
        if (experienceValue == null) {
            experienceValue = new MethodNode(Opcodes.ACC_PUBLIC, "getExperienceValue", "()I", null, null);
            InsnList insn = experienceValue.instructions;
            LabelNode allowDrop = new LabelNode();
            insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
            insn.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isDropExperienceOrbsOnDeath", "()Z"));
            insn.add(new JumpInsnNode(Opcodes.IFNE, allowDrop));
            insn.add(new InsnNode(Opcodes.ICONST_0));
            insn.add(new InsnNode(Opcodes.IRETURN));
            insn.add(allowDrop);
            insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insn.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, entityPlayerSuper, "getExperienceValue", "()I"));
            insn.add(new InsnNode(Opcodes.IRETURN));
            cn.methods.add(experienceValue);
        } else {
            boolean hasGuard = false;
            for (AbstractInsnNode insn = experienceValue.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode node = (MethodInsnNode) insn;
                    if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                            && "cn/flowerinsnow/miteoperator/config/AgentConfig".equals(node.owner)
                            && "isDropExperienceOrbsOnDeath".equals(node.name)
                            && "()Z".equals(node.desc)) {
                        hasGuard = true;
                        break;
                    }
                }
            }
            if (!hasGuard) {
                InsnList guard = new InsnList();
                LabelNode allowDrop = new LabelNode();
                guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
                guard.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isDropExperienceOrbsOnDeath", "()Z"));
                guard.add(new JumpInsnNode(Opcodes.IFNE, allowDrop));
                guard.add(new InsnNode(Opcodes.ICONST_0));
                guard.add(new InsnNode(Opcodes.IRETURN));
                guard.add(allowDrop);
                experienceValue.instructions.insert(guard);
            }
        }

        for (MethodNode mn : cn.methods) {
            if (!mn.desc.endsWith(")V")) {
                continue;
            }

            InsnList instructions = mn.instructions;
            boolean touchesRespawnFields = false;
            for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;
                    if (fieldInsnNode.getOpcode() == Opcodes.PUTFIELD
                            && (("respawn_experience".equals(fieldInsnNode.name) && "I".equals(fieldInsnNode.desc))
                            || ("respawn_countdown".equals(fieldInsnNode.name) && "S".equals(fieldInsnNode.desc)))) {
                        touchesRespawnFields = true;
                        break;
                    }
                }
            }
            if (!touchesRespawnFields) {
                continue;
            }

            for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof FieldInsnNode)) {
                    continue;
                }
                FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;
                if (fieldInsnNode.getOpcode() != Opcodes.GETFIELD || fieldInsnNode.desc == null || !fieldInsnNode.desc.startsWith("L")) {
                    continue;
                }
                AbstractInsnNode next = getNextRealInsn(fieldInsnNode);
                if (!(next instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode node = (MethodInsnNode) next;
                String fieldOwner = fieldInsnNode.desc.substring(1, fieldInsnNode.desc.length() - 1);
                if (node.getOpcode() != Opcodes.INVOKEVIRTUAL || !"()V".equals(node.desc) || !fieldOwner.equals(node.owner)) {
                    continue;
                }
                LabelNode skipDrop = new LabelNode();
                InsnList guard = new InsnList();
                guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
                guard.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isKeepInventoryOnDeath", "()Z"));
                guard.add(new JumpInsnNode(Opcodes.IFNE, skipDrop));
                instructions.insertBefore(fieldInsnNode, guard);
                instructions.insert(node, skipDrop);
                break;
            }

            for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.RETURN) {
                    InsnList tail = new InsnList();
                        LabelNode skipExperience = new LabelNode();
                        tail.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
                        tail.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isKeepExperienceOnDeath", "()Z"));
                        tail.add(new JumpInsnNode(Opcodes.IFEQ, skipExperience));
                        if (experienceField != null) {
                            tail.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            tail.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            tail.add(new FieldInsnNode(Opcodes.GETFIELD, experienceField.owner, experienceField.name, experienceField.desc));
                            tail.add(new FieldInsnNode(Opcodes.PUTFIELD, cn.name, "respawn_experience", "I"));
                        }
                        tail.add(skipExperience);

                    tail.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    tail.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
                    tail.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "getRespawnCountdownSeconds", "()I"));
                    tail.add(new InsnNode(Opcodes.I2S));
                    tail.add(new FieldInsnNode(Opcodes.PUTFIELD, cn.name, "respawn_countdown", "S"));

                    instructions.insertBefore(insn, tail);
                }
            }
        }

        for (MethodNode mn : cn.methods) {
            if (!entityPlayerDesc.equals(mn.desc)) {
                continue;
            }
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode node = (MethodInsnNode) insn;
                    if (node.getOpcode() == Opcodes.INVOKESPECIAL && entityPlayerSuper.equals(node.owner) && entityPlayerDesc.equals(node.desc)) {
                        InsnList keepInventory = new InsnList();
                        LabelNode skipInventory = new LabelNode();
                        keepInventory.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
                        keepInventory.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isKeepInventoryOnDeath", "()Z"));
                        keepInventory.add(new JumpInsnNode(Opcodes.IFEQ, skipInventory));
                        keepInventory.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        keepInventory.add(new FieldInsnNode(Opcodes.GETFIELD, entityPlayerSuper, "bn", "Lud;"));
                        keepInventory.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        keepInventory.add(new FieldInsnNode(Opcodes.GETFIELD, entityPlayerSuper, "bn", "Lud;"));
                        keepInventory.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "ud", "b", "(Lud;)V"));
                        keepInventory.add(skipInventory);

                        InsnList keepExperience = new InsnList();
                        LabelNode skipExperience = new LabelNode();
                        keepExperience.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
                        keepExperience.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isKeepExperienceOnDeath", "()Z"));
                        keepExperience.add(new JumpInsnNode(Opcodes.IFEQ, skipExperience));
                        keepExperience.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        keepExperience.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        keepExperience.add(new FieldInsnNode(Opcodes.GETFIELD, entityPlayerSuper, "bJ", "I"));
                        keepExperience.add(new FieldInsnNode(Opcodes.PUTFIELD, entityPlayerSuper, "bJ", "I"));
                        keepExperience.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        keepExperience.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        keepExperience.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, entityPlayerSuper, "bw", "()I"));
                        keepExperience.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, entityPlayerSuper, "c", "(I)V"));
                        keepExperience.add(skipExperience);

                        InsnList keepHunger = new InsnList();
                        LabelNode skipHunger = new LabelNode();
                        keepHunger.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cn/flowerinsnow/miteoperator/config/AgentConfig", "get", "()Lcn/flowerinsnow/miteoperator/config/AgentConfig;"));
                        keepHunger.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cn/flowerinsnow/miteoperator/config/AgentConfig", "isKeepHungerOnDeath", "()Z"));
                        keepHunger.add(new JumpInsnNode(Opcodes.IFEQ, skipHunger));
                        if (foodStatsField != null) {
                            keepHunger.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            keepHunger.add(new VarInsnNode(Opcodes.ALOAD, 1));
                            keepHunger.add(new FieldInsnNode(Opcodes.GETFIELD, foodStatsField.owner, foodStatsField.name, foodStatsField.desc));
                            keepHunger.add(new FieldInsnNode(Opcodes.PUTFIELD, foodStatsField.owner, foodStatsField.name, foodStatsField.desc));
                        }
                        keepHunger.add(skipHunger);

                        mn.instructions.insert(node, keepInventory);
                        mn.instructions.insert(node, keepExperience);
                        mn.instructions.insert(node, keepHunger);
                        break;
                    }
                }
            }
            break;
        }
    }

    private boolean looksLikeEntityPlayerMP(byte[] bytes) {
        try {
            ClassReader cr = new ClassReader(bytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            boolean hasRespawnExperience = cn.fields.stream()
                    .anyMatch(field -> "respawn_experience".equals(field.name) && "I".equals(field.desc));
            boolean hasClonePlayer = cn.methods.stream()
                    .anyMatch(method -> "clonePlayer".equals(method.name));
            boolean hasOnDeath = cn.methods.stream()
                    .anyMatch(method -> "onDeath".equals(method.name) && method.desc.endsWith(")V"));
            return hasRespawnExperience && hasClonePlayer && hasOnDeath;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private AbstractInsnNode getNextRealInsn(AbstractInsnNode node) {
        AbstractInsnNode next = node.getNext();
        while (next != null && (next.getType() == AbstractInsnNode.LINE || next.getType() == AbstractInsnNode.FRAME || next.getType() == AbstractInsnNode.LABEL)) {
            next = next.getNext();
        }
        return next;
    }

    private AbstractInsnNode getPrevRealInsn(AbstractInsnNode node) {
        AbstractInsnNode prev = node.getPrevious();
        while (prev != null && (prev.getType() == AbstractInsnNode.LINE || prev.getType() == AbstractInsnNode.FRAME || prev.getType() == AbstractInsnNode.LABEL)) {
            prev = prev.getPrevious();
        }
        return prev;
    }

    private ExperienceField resolveExperienceField(ClassNode cn, String entityPlayerSuper) {
        for (MethodNode mn : cn.methods) {
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof FieldInsnNode)) {
                    continue;
                }
                FieldInsnNode getField = (FieldInsnNode) insn;
                if (getField.getOpcode() != Opcodes.GETFIELD || !"I".equals(getField.desc)) {
                    continue;
                }
                AbstractInsnNode cursor = getNextRealInsn(getField);
                if (!(cursor instanceof InsnNode) || cursor.getOpcode() != Opcodes.ICONST_1) {
                    continue;
                }
                cursor = getNextRealInsn(cursor);
                if (!(cursor instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode methodInsnNode = (MethodInsnNode) cursor;
                if (methodInsnNode.getOpcode() != Opcodes.INVOKESTATIC || !"getExperienceRequired".equals(methodInsnNode.name) || !"(I)I".equals(methodInsnNode.desc)) {
                    continue;
                }
                cursor = getNextRealInsn(cursor);
                if (!(cursor instanceof InsnNode) || cursor.getOpcode() != Opcodes.ISUB) {
                    continue;
                }
                cursor = getNextRealInsn(cursor);
                if (!(cursor instanceof FieldInsnNode)) {
                    continue;
                }
                FieldInsnNode putField = (FieldInsnNode) cursor;
                if (putField.getOpcode() != Opcodes.PUTFIELD || !"respawn_experience".equals(putField.name) || !"I".equals(putField.desc)) {
                    continue;
                }
                return new ExperienceField(getField.owner, getField.name, getField.desc);
            }
        }

        for (MethodNode mn : cn.methods) {
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode node = (FieldInsnNode) insn;
                    if ("experience".equals(node.name) && "I".equals(node.desc)) {
                        return new ExperienceField(node.owner, node.name, node.desc);
                    }
                }
            }
        }

        return null;
    }

    private FoodStatsField resolveFoodStatsField(ClassNode cn, String entityPlayerSuper) {
        for (MethodNode mn : cn.methods) {
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof FieldInsnNode)) {
                    continue;
                }
                FieldInsnNode node = (FieldInsnNode) insn;
                if (node.getOpcode() == Opcodes.GETFIELD
                        && entityPlayerSuper.equals(node.owner)
                        && "Luc;".equals(node.desc)) {
                    return new FoodStatsField(node.owner, node.name, node.desc);
                }
            }
        }
        return null;
    }

    private static final class ExperienceField {
        private final String owner;
        private final String name;
        private final String desc;

        private ExperienceField(String owner, String name, String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }
    }

    private static final class FoodStatsField {
        private final String owner;
        private final String name;
        private final String desc;

        private FoodStatsField(String owner, String name, String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }
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
        try (InputStream in = MITEOperatorAgent.class.getClassLoader().getResourceAsStream(path)) {
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

    private static String findFieldDesc(ClassNode cn, String fieldName) {
        for (MethodNode mn : cn.methods) {
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode node = (FieldInsnNode) insn;
                    if (fieldName.equals(node.name)) {
                        return node.desc;
                    }
                }
            }
        }
        return null;
    }

    private static boolean methodAccessesField(MethodNode mn, String fieldName) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode) {
                if (fieldName.equals(((FieldInsnNode) insn).name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
