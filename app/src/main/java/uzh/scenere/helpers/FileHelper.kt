package uzh.scenere.helpers

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import android.content.Intent
import android.net.Uri


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

        fun readFile(context: Context, fileName: String): ByteArray{
            val file = File(context.filesDir, fileName)
            return file.readBytes()
        }

        fun openFolder(context: Context, uri: String){
            val selectedUri = Uri.parse(uri)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(selectedUri, "resource/folder")
            if (intent.resolveActivityInfo(context.packageManager, 0) != null){
                context.startActivity(intent);
            }
        }

    }
}