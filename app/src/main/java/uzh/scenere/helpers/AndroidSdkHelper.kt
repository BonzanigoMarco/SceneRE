package uzh.scenere.helpers

import uzh.scenere.const.Constants

//AndroidSdkHelper.setRColor(R::class.java,"srePrimaryLight",R.color.sreAccentLight)
//AndroidSdkHelper.setRColor(R::class.java,"srePrimary",R.color.sreAccent)
//AndroidSdkHelper.setRColor(R::class.java,"srePrimaryDark",R.color.sreAccentDark)
class AndroidSdkHelper private constructor() {
    companion object {
        fun setRColor(rClass: Class<*>, rFieldName: String, newValue: Any) {
            setR(rClass, "color", rFieldName, newValue)
        }

        fun setRString(rClass: Class<*>, rFieldName: String, newValue: Any) {
            setR(rClass, "string", rFieldName, newValue)
        }

        fun setR(rClass: Class<*>, innerClassName: String, rFieldName: String, newValue: Any) {
            setStatic(rClass.name + Constants.DOLLAR_STRING + innerClassName, rFieldName, newValue)
        }

        private fun setStatic(aClassName: String, staticFieldName: String, toSet: Any): Boolean {
            return try {
                setStatic(Class.forName(aClassName), staticFieldName, toSet)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                false
            }

        }

        private fun setStatic(aClass: Class<*>, staticFieldName: String, toSet: Any): Boolean {
            return try {
                val declaredField = aClass.getDeclaredField(staticFieldName)
                declaredField.isAccessible = true
                declaredField.set(null, toSet)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }

        }
    }
}