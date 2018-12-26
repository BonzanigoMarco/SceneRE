package uzh.scenere.datamodel

class Resource private constructor(private val name: String, private val description: String) {
    private var max: Double? = null
    private var min: Double? = null
    private var init: Double? = null
    private var diff: Double? = null

    class ResourceBuilder(val name: String, val description: String){
        private var max: Double? = null
        private var min: Double? = null
        private var init: Double? = null
        private var diff: Double? = null

        fun configure(min: Double?, max: Double?, init: Double?, diff: Double?): Resource{
            this.max = max
            this.min = min
            this.init = init
            this.diff = diff
            return build()
        }
        private fun build(): Resource{
            val resource  = Resource(name,description)
            resource.max = this.max
            resource.min = this.min
            resource.init = this.init
            resource.diff = this.diff
            return resource
        }
    }

}