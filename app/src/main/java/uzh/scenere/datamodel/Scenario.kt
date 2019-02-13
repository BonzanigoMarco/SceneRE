package uzh.scenere.datamodel

import android.content.Context
import com.google.android.gms.common.util.NumberUtils
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.NumberHelper
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class Scenario private constructor(val id: String, val projectId: String, val title: String, val intro: String, val outro: String): Serializable {
    private val resources: List<Resource> = ArrayList()
    val objects: ArrayList<AbstractObject> = ArrayList()
    var paths: HashMap<String,HashMap<Int,Path>> = HashMap()
    private val stakeholders: List<Stakeholder> = ArrayList()
    private val walkthroughs: List<Walkthrough> = ArrayList()
    private val whatIfs: List<WhatIf> = ArrayList()

    fun getPath(stakeholder: Stakeholder, context: Context, layer: Int = -1 ): Path {
        val stakeholderPaths = paths[stakeholder.id]
        return if (stakeholderPaths == null){
            val path = Path.PathBuilder(id, stakeholder, 0).build()
            DatabaseHelper.getInstance(context).write(path.id,path)
            val initializedPath = hashMapOf(0 to path)
            paths[stakeholder.id] = initializedPath
            initializedPath[0]!!
        }else{
            if (layer == -1){
                stakeholderPaths[0]!!
            }else{
                stakeholderPaths[layer]!!
            }
        }
    }

    fun getAllPaths(stakeholder: Stakeholder): HashMap<Int, Path>? {
        return paths[stakeholder.id]
    }

    fun getAllPaths(): List<Path> {
        val pathList = ArrayList<Path>()
        for (entry in paths.entries){
            for (pathEntry in entry.value.entries){
                pathList.add(pathEntry.value)
            }
        }
        return pathList
    }

    fun updatePath(stakeholder: Stakeholder, path: Path ){
        if (paths[stakeholder.id] != null){
            paths[stakeholder.id]!![path.layer] = path
        }else{
            paths[stakeholder.id] = hashMapOf(path.layer to path)
        }
    }

    fun removePath(stakeholder: Stakeholder, path: Path ){
        if (paths[stakeholder.id] != null){
            paths[stakeholder.id]!![path.layer] = Path.PathBuilder(id, stakeholder, path.layer).build()
        }
    }

    fun getObjectNames(vararg additionalName: String): Array<String>{
        val list = ArrayList<String>()
        list.addAll(additionalName)
        for (obj in objects){
            list.add(obj.name)
        }
        return list.toTypedArray()
    }

    fun getObjectByName(name: String?): AbstractObject?{
        for (obj in objects){
            if (obj.name == name){
                return obj
            }
        }
        return null
    }

    fun hasStakeholderPath(stakeholder: Stakeholder): Boolean{
        val pathMap = paths[stakeholder.id]
        if (!pathMap.isNullOrEmpty() && NumberHelper.nvl(pathMap[0]?.elements?.size,0)>0){
            return true
        }
        return false
    }

    class ScenarioBuilder(private val projectId: String, private val title: String, private val intro: String, private val outro: String){

        constructor(project: Project, title: String, intro: String, outro: String): this(project.id,title,intro,outro)

        constructor(id: String, project: Project, title: String, intro: String, outro: String): this(project,title,intro,outro){
            this.id = id
        }

        constructor(id: String, projectId: String, title: String, intro: String, outro: String): this(projectId,title,intro,outro){
            this.id = id
        }

        private var id: String? = null
        private var resources: List<Resource> = ArrayList()
        private var objects: List<AbstractObject> = ArrayList()
        private val paths: HashMap<String,HashMap<Int,Path>> = HashMap()
        private var stakeholders: List<Stakeholder> = ArrayList()
        private var walkthroughs: List<Walkthrough> = ArrayList()
        private var whatIfs: List<WhatIf> = ArrayList()

//        fun addResource(vararg resource: Resource): ScenarioBuilder{
//            this.resources = this.resources.plus(resource)
//            return this
//        }
        fun addObjects(vararg obj: AbstractObject): ScenarioBuilder{
            this.objects = this.objects.plus(obj)
            return this
        }
        fun addPaths(vararg path: Path): ScenarioBuilder{
            for (p in path){
                if (paths[p.stakeholder.id] == null){
                    this.paths[p.stakeholder.id] = hashMapOf(p.layer to p)
                }else{
                    this.paths[p.stakeholder.id]!![p.layer] = p
                }
            }
            return this
        }
//        fun addStakeholder(vararg stakeholder: Stakeholder): ScenarioBuilder{
//            this.stakeholders = this.stakeholders.plus(stakeholder)
//            return this
//        }
//        fun addWalkthrough(vararg walkthrough: Walkthrough): ScenarioBuilder{
//            this.walkthroughs = this.walkthroughs.plus(walkthrough)
//            return this
//        }
//        fun addWhatIf(vararg whatIf: WhatIf): ScenarioBuilder{
//            this.whatIfs = this.whatIfs.plus(whatIf)
//            return this
//        }
        fun copyId(scenario: Scenario) {
            this.id = scenario.id
        }

        fun build(): Scenario{
            val scenario  = Scenario(id?: UUID.randomUUID().toString(),projectId, title, intro, outro)
//            scenario.resources.plus(this.resources)
            scenario.objects.addAll(this.objects)
            scenario.paths = this.paths
//            scenario.stakeholders.plus(this.stakeholders)
//            scenario.walkthroughs.plus(this.walkthroughs)
//            scenario.whatIfs.plus(this.whatIfs)
//            scenario.startingPoint = getStartingPoint()
            return scenario
        }
//        private fun getStartingPoint(): AbstractStep?{
//            for (path in paths){
//                val step = path.getStartingPoint()
//                if (step != null) return step
//            }
//            return null
//        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Scenario){
            return (id == other.id)
        }
        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    fun getObjectsWithNames(objectNames: ArrayList<String>): ArrayList<AbstractObject>{
        val objectList = ArrayList<AbstractObject>()
        for (name in objectNames){
            for (obj in this.objects){
                if (obj.name == name){
                    objectList.add(obj)
                }
            }
        }
        return objectList
    }

    class NullScenario(): Scenario("","","","","") {}
}