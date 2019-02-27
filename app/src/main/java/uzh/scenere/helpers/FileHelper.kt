package uzh.scenere.helpers

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import android.content.Intent
import android.net.Uri
import uzh.scenere.const.Constants.Companion.NOTHING
import java.lang.Exception


class FileHelper {

    companion object {
        fun writeFile(context: Context, str: String, fileName: String): String{
            return writeFile(context,str.toByteArray(),fileName)
        }

        fun writeFile(context: Context, array: ByteArray, fileName: String): String{
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(array)
            }
            return context.filesDir.path
        }

        fun getDefaultFilePath(context: Context): String{
            return context.filesDir.path
        }

        fun readFile(context: Context, filePath: String): ByteArray{
            var bytes: ByteArray? = null
            try{
                val file = File(filePath)
                if (file.exists()){
                    bytes =  file.readBytes()
                }
            }catch (e: Exception){

            }
            return bytes ?: ByteArray(0)
        }

        fun openFolder(context: Context, uri: String){
            val selectedUri = Uri.parse(uri)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(selectedUri, "resource/folder")
            if (intent.resolveActivityInfo(context.packageManager, 0) != null){
                context.startActivity(intent);
            }
        }

        fun removeFileFromPath(filePath: String?): String {
            if (!StringHelper.hasText(filePath)){
                return NOTHING
            }
            val split = filePath!!.split("/")
            return filePath.replace("/".plus(split[split.size-1]),NOTHING)
        }

        fun getFilesInFolder(folderPath: String): Array<out File> {
            val folder = File(folderPath)
            if (folder.isDirectory){
                return folder.listFiles()
            }
            return emptyArray()
        }

    }
}