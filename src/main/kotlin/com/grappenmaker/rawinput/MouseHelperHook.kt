package com.grappenmaker.rawinput

import net.weavemc.api.Hook
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

@Suppress("unused")
// TODO: mixin?
class MouseHelperHook : Hook("net/minecraft/util/MouseHelper") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) = with(node) {
        transform("<init>") {
            instructions.insertBefore(instructions.find { it.opcode == RETURN }, asm {
                getstatic(internalNameOf<RawInputThread>(), "INSTANCE", "L${internalNameOf<RawInputThread>()};")
                invokevirtual("java/lang/Thread", "start", "()V")
            })
        }

        transform("mouseXYChange") {
            val calls = instructions.filterIsInstance<MethodInsnNode>()

            fun replaceMouseCall(targetMethod: String, propertyMethod: String) {
                val target = calls.find { it.name == targetMethod } ?: return

                instructions.insert(target, asm {
                    invokestatic(internalNameOf<RawInputThread>(), propertyMethod, "()F")
                    f2i
                })

                instructions.remove(target)
            }

            replaceMouseCall("getDX", "getDx")
            replaceMouseCall("getDY", "getDy")
        }

        transform("grabMouseCursor") {
            instructions.insertBefore(instructions.find { it.opcode == RETURN }, asm {
                invokestatic(internalNameOf<RawInputThread>(), "resetMouse", "()V")
            })
        }
    }
}

inline fun ClassNode.transform(name: String, block: MethodNode.() -> Unit) = block(methods.named(name))