package com.example.gradle_plugin.trace

import com.example.gradle_plugin.util.Log
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class TraceClassVisitor(
    cv: ClassVisitor?,
    private val traceConfig: Config
) : ClassVisitor(Opcodes.ASM9, cv) {

    private var className = ""
    private var isAbsClass = false
    private var isBeatClass = false

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        className = name?.replace('/', '.') ?: ""

        if (traceConfig.mBeatClass == className) {
            isBeatClass = true
        }

        isAbsClass = (access and Opcodes.ACC_ABSTRACT != 0) || (access and Opcodes.ACC_INTERFACE != 0)

        if (traceConfig.mIsNeedLogTraceInfo && !isAbsClass && !isBeatClass) {
            Log.log("TraceClassVisitor::visit => $className")
        }
    }

    override fun visitMethod(
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<String>?
    ): MethodVisitor {
        if (isConstructor(name)|| isStaticInit(name) || isAbsClass || isBeatClass) {
            return super.visitMethod(access, name, desc, signature, exceptions)
        }
        val mv = cv.visitMethod(access, name, desc, signature, exceptions)
        return TraceMethodVisitor(Opcodes.ASM9, mv, access, name, desc, className, traceConfig)
    }

    private fun isStaticInit(name: String): Boolean = name == "<clinit>"

    private fun isConstructor(name: String): Boolean = name == "<init>"
}
