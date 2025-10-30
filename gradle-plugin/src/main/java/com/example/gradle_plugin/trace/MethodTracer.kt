//package com.example.gradle_plugin.trace
//
//import com.android.build.api.transform.*
//import com.example.gradle_plugin.util.Utils
//import groovyjarjarasm.asm.ClassReader.EXPAND_FRAMES
//import org.apache.commons.codec.digest.DigestUtils
//import org.apache.commons.io.FileUtils
//import org.apache.commons.io.IOUtils
//import org.objectweb.asm.ClassReader
//import org.objectweb.asm.ClassWriter
//import org.objectweb.asm.Opcodes
//import java.io.File
//import java.io.FileOutputStream
//import java.util.*
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//import java.util.concurrent.Future
//import java.util.jar.JarFile
//import java.util.jar.JarOutputStream
//import java.util.zip.ZipEntry
//
///**
// *  author : zhang.wenqiang
// *  date : 2021/10/22
// *  description :
// */
//class MethodTracer(
//    val config: Config
//) {
//
//    private val executor: ExecutorService = Executors.newFixedThreadPool(16)
//
//    fun trace(input: TransformInput, outputProvider: TransformOutputProvider) {
//        val start = System.currentTimeMillis()
//
//        val futures: MutableList<Future<*>> = LinkedList()
//        input.jarInputs.forEach {
//            traceJarFiles(it, outputProvider, futures)
//        }
//        input.directoryInputs.forEach {
//            traceDirectoryFiles(it, outputProvider, futures)
//        }
//        for (future in futures) {
//            future.get()
//        }
//        futures.clear()
//    }
//
//    private fun traceDirectoryFiles(
//        directoryInput: DirectoryInput,
//        outputProvider: TransformOutputProvider,
//        futures: MutableList<Future<*>>
//    ) {
//        val classFileList = ArrayList<File>()
//        if (directoryInput.file.isDirectory) {
//            Utils.listClassFiles(classFileList, directoryInput.file)
//        } else {
//            classFileList.add(directoryInput.file)
//        }
//
//        classFileList.forEach {
//            if (config.isNeedTraceClass(it.name)) {
//                futures.add(executor.submit(CollectDirectoryTask(it)))
//            }
//        }
//
//        val dest = outputProvider.getContentLocation(
//            directoryInput.name,
//            directoryInput.contentTypes, directoryInput.scopes,
//            Format.DIRECTORY
//        )
//        FileUtils.copyDirectory(directoryInput.file, dest)
//    }
//
//    private fun traceJarFiles(
//        jarInput: JarInput,
//        outputProvider: TransformOutputProvider,
//        futures: MutableList<Future<*>>
//    ) {
//        futures.add(executor.submit(CollectJarTask(jarInput, outputProvider)))
//    }
//
//    internal inner class CollectDirectoryTask(private val it: File) : Runnable {
//        override fun run() {
//            val classReader = ClassReader(it.readBytes())
//            val classWriter = ClassWriter(
//                classReader,
//                ClassWriter.COMPUTE_MAXS
//            )
//            val cv = TraceClassVisitor(
//                Opcodes.ASM7,
//                classWriter,
//                config
//            )
//            classReader.accept(cv, EXPAND_FRAMES)
//            val code = classWriter.toByteArray()
//            val fos = FileOutputStream(
//                it.parentFile.absolutePath + File.separator + it.name
//            )
//            fos.write(code)
//            fos.close()
//        }
//    }
//
//    internal inner class CollectJarTask(
//        private val jarInput: JarInput,
//        private val outputProvider: TransformOutputProvider,
//    ) : Runnable {
//        override fun run() {
//            if (jarInput.file.absolutePath.endsWith(".jar")) {
//                //重命名输出文件,因为可能同名,会覆盖
//                var jarName = jarInput.name
//                val md5Name = DigestUtils.md5Hex(jarInput.file.absolutePath)
//                if (jarName.endsWith(".jar")) {
//                    jarName = jarName.substring(0, jarName.length - 4)
//                }
//                val jarFile = JarFile(jarInput.file)
//                val enumeration = jarFile.entries()
//
//                val tmpFile = File(jarInput.file.parent + File.separator + "classes_temp.jar")
//                if (tmpFile.exists()) {
//                    tmpFile.delete()
//                }
//
//                val jarOutputStream = JarOutputStream(FileOutputStream(tmpFile))
//
//                while (enumeration.hasMoreElements()) {
//                    val jarEntry = enumeration.nextElement()
//                    val entryName = jarEntry.name
//                    val zipEntry = ZipEntry(entryName)
//                    val inputStream = jarFile.getInputStream(jarEntry)
//                    if (config.isNeedTraceClass(entryName)) {
//                        jarOutputStream.putNextEntry(zipEntry)
//                        val classReader = ClassReader(IOUtils.toByteArray(inputStream))
//                        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
//                        val cv =
//                            TraceClassVisitor(
//                                Opcodes.ASM7,
//                                classWriter,
//                                config
//                            )
//                        classReader.accept(cv, EXPAND_FRAMES)
//                        val code = classWriter.toByteArray()
//                        jarOutputStream.write(code)
//                    } else {
//                        jarOutputStream.putNextEntry(zipEntry)
//                        jarOutputStream.write(IOUtils.toByteArray(inputStream))
//                    }
//                    jarOutputStream.closeEntry()
//                }
//                jarOutputStream.close()
//                jarFile.close()
//                val dest = outputProvider.getContentLocation(
//                    jarName + md5Name,
//                    jarInput.contentTypes, jarInput.scopes, Format.JAR
//                )
//                FileUtils.copyFile(tmpFile, dest)
//                tmpFile.delete()
//            }
//        }
//    }
//}