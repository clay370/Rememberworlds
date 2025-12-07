package com.example.rememberworlds.utils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ZipUtils {

    /**
     * Unzips a zip file to the specified destination directory.
     * Returns a list of extracted files.
     */
    fun unzip(zipFile: File, targetDirectory: File): List<File> {
        val extractedFiles = mutableListOf<File>()
        
        // Ensure target directory exists
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var ze = zis.nextEntry
            while (ze != null) {
                val file = File(targetDirectory, ze.name)
                
                // Ensure the entry is within the target directory to prevent Zip Slip vulnerability
                if (!file.canonicalPath.startsWith(targetDirectory.canonicalPath)) {
                    throw SecurityException("Zip entry is outside of the target dir: " + ze.name)
                }

                if (ze.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        val buffer = ByteArray(1024)
                        var count: Int
                        while (zis.read(buffer).also { count = it } != -1) {
                            fos.write(buffer, 0, count)
                        }
                    }
                    extractedFiles.add(file)
                }
                ze = zis.nextEntry
            }
        }
        return extractedFiles
    }
}
