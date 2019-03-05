package uzh.scenere.datamodel.database

import android.content.ContentValues
import android.content.Context
import uzh.scenere.const.Constants.Companion.ARRAY_LIST_WHAT_IF_IDENTIFIER
import uzh.scenere.const.Constants.Companion.HASH_MAP_LINK_IDENTIFIER
import uzh.scenere.const.Constants.Companion.HASH_MAP_OPTIONS_IDENTIFIER
import uzh.scenere.const.Constants.Companion.INIT_IDENTIFIER
import uzh.scenere.const.Constants.Companion.MAX_IDENTIFIER
import uzh.scenere.const.Constants.Companion.MIN_IDENTIFIER
import uzh.scenere.const.Constants.Companion.TYPE_BUTTON_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_IF_ELSE_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_OBJECT
import uzh.scenere.const.Constants.Companion.TYPE_STANDARD_STEP
import uzh.scenere.const.Constants.Companion.VERSIONING_IDENTIFIER
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.steps.StandardStep
import uzh.scenere.datamodel.trigger.AbstractTrigger
import uzh.scenere.datamodel.trigger.direct.ButtonTrigger
import uzh.scenere.datamodel.trigger.direct.IfElseTrigger
import uzh.scenere.helpers.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SreDatabase private constructor(context: Context) : AbstractSreDatabase() {
    private val dbHelper: DbHelper = DbHelper(context)

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @Volatile
        private var instance: SreDatabase? = null

        fun getInstance(context: Context): SreDatabase {
            return when {
                instance != null -> instance!!
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = SreDatabase(context)
                    }
                    instance!!
                }
            }
        }
    }

    /** WRITE    **
     ** TO       **
     ** DATABASE **/
    fun writeByteArray(key: String, value: ByteArray): Long {
        val values = ContentValues()
        values.put(DataTableEntry.KEY, key)
        values.put(DataTableEntry.VALUE, value)
        return insert(DataTableEntry.TABLE_NAME, DataTableEntry.KEY, key, values)
    }

    fun writeString(key: String, value: String): Long {
        val values = ContentValues()
        values.put(TextTableEntry.KEY, key)
        values.put(TextTableEntry.VALUE, value)
        return insert(TextTableEntry.TABLE_NAME, TextTableEntry.KEY, key, values)
    }

    fun writeBoolean(key: String, value: Boolean): Long {
        val values = ContentValues()
        values.put(DataTableEntry.KEY, key)
        values.put(DataTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeShort(key: String, value: Short): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.KEY, key)
        values.put(NumberTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeInt(key: String, value: Int): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.KEY, key)
        values.put(NumberTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeLong(key: String, value: Long): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.KEY, key)
        values.put(NumberTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeFloat(key: String, value: Float): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.KEY, key)
        values.put(NumberTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeDouble(key: String, value: Double): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.KEY, key)
        values.put(NumberTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeProject(project: Project): Long {
        addVersioning(project.id)
        val values = ContentValues()
        values.put(ProjectTableEntry.ID, project.id)
        values.put(ProjectTableEntry.CREATOR, project.creator)
        values.put(ProjectTableEntry.TITLE, project.title)
        values.put(ProjectTableEntry.DESCRIPTION, project.description)
        return insert(ProjectTableEntry.TABLE_NAME, ProjectTableEntry.ID, project.id, values)
    }

    fun writeStakeholder(stakeholder: Stakeholder): Long {
        addVersioning(stakeholder.id)
        val values = ContentValues()
        values.put(StakeholderTableEntry.ID, stakeholder.id)
        values.put(StakeholderTableEntry.PROJECT_ID, stakeholder.projectId)
        values.put(StakeholderTableEntry.NAME, stakeholder.name)
        values.put(StakeholderTableEntry.DESCRIPTION, stakeholder.description)
        return insert(StakeholderTableEntry.TABLE_NAME, StakeholderTableEntry.ID, stakeholder.id, values)
    }

    fun writeObject(obj: AbstractObject): Long {
        addVersioning(obj.id)
        val values = ContentValues()
        values.put(ObjectTableEntry.ID, obj.id)
        values.put(ObjectTableEntry.SCENARIO_ID, obj.scenarioId)
        values.put(ObjectTableEntry.NAME, obj.name)
        values.put(ObjectTableEntry.DESCRIPTION, obj.description)
        values.put(ObjectTableEntry.IS_RESOURCE, obj.isResource)
        for (attribute in obj.attributes) {
            writeAttribute(attribute)
        }
        if (obj is Resource){
            writeDouble(MIN_IDENTIFIER.plus(obj.id),obj.min)
            writeDouble(MAX_IDENTIFIER.plus(obj.id),obj.max)
            writeDouble(INIT_IDENTIFIER.plus(obj.id),obj.init)
        }
        return insert(ObjectTableEntry.TABLE_NAME, ObjectTableEntry.ID, obj.id, values)
    }

    fun writeAttribute(attribute: Attribute): Long {
        addVersioning(attribute.id)
        val values = ContentValues()
        values.put(AttributeTableEntry.ID, attribute.id)
        values.put(AttributeTableEntry.REF_ID, attribute.refId)
        values.put(AttributeTableEntry.KEY, attribute.key)
        values.put(AttributeTableEntry.VALUE, attribute.value)
        values.put(AttributeTableEntry.TYPE, attribute.type)
        return insert(AttributeTableEntry.TABLE_NAME, AttributeTableEntry.ID, attribute.id, values)
    }

    fun writeScenario(scenario: Scenario): Long {
        addVersioning(scenario.id)
        val values = ContentValues()
        values.put(ScenarioTableEntry.ID, scenario.id)
        values.put(ScenarioTableEntry.PROJECT_ID, scenario.projectId)
        values.put(ScenarioTableEntry.TITLE, scenario.title)
        values.put(ScenarioTableEntry.INTRO, scenario.intro)
        values.put(ScenarioTableEntry.OUTRO, scenario.outro)
        return insert(ScenarioTableEntry.TABLE_NAME, ScenarioTableEntry.ID, scenario.id, values)
    }

    fun writeElement(element: IElement): Long {
        val values = ContentValues()
        values.put(ElementTableEntry.ID, element.getElementId())
        values.put(ElementTableEntry.PREV_ID, element.getPreviousElementId())
        values.put(ElementTableEntry.PATH_ID, element.getElementPathId())
        if (element is AbstractStep) {
            addVersioning(element.id)
            values.put(ElementTableEntry.TITLE, element.title)
            values.put(ElementTableEntry.TEXT, element.text)
            writeByteArray(ARRAY_LIST_WHAT_IF_IDENTIFIER.plus(element.getElementId()),DataHelper.toByteArray(element.whatIfs))
        }else if (element is AbstractTrigger){
            addVersioning(element.id)
        }
        when (element) {
            is StandardStep -> {
                for (obj in element.objects) {
                    writeAttribute(Attribute.AttributeBuilder(element.id, obj.id, null).withAttributeType(TYPE_OBJECT).build())
                }
                values.put(ElementTableEntry.TYPE, TYPE_STANDARD_STEP)
            }
            is ButtonTrigger -> {
                values.put(ElementTableEntry.TITLE, element.buttonLabel)
                values.put(ElementTableEntry.TYPE, TYPE_BUTTON_TRIGGER)
            }
            is IfElseTrigger -> {
                values.put(ElementTableEntry.TITLE, element.defaultOption)
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_IF_ELSE_TRIGGER)
                writeByteArray(HASH_MAP_OPTIONS_IDENTIFIER.plus(element.getElementId()),DataHelper.toByteArray(element.pathOptions))
                writeByteArray(HASH_MAP_LINK_IDENTIFIER.plus(element.getElementId()),DataHelper.toByteArray(element.optionLayerLink))
            }
        }
        return insert(ElementTableEntry.TABLE_NAME, ElementTableEntry.ID, element.getElementId(), values)
    }

    fun writePath(path: Path): Long {
        addVersioning(path.id)
        val values = ContentValues()
        values.put(PathTableEntry.ID, path.id)
        values.put(PathTableEntry.SCENARIO_ID, path.scenarioId)
        values.put(PathTableEntry.STAKEHOLDER_ID, path.stakeholder.id)
        values.put(PathTableEntry.LAYER, path.layer)
        for (entry in path.elements.entries){
            writeElement(entry.value)
        }
        return insert(PathTableEntry.TABLE_NAME, PathTableEntry.ID, path.id, values)
    }

    fun writeWalkthrough(walkthrough: Walkthrough): Long {
        addVersioning(walkthrough.id)
        val values = ContentValues()
        values.put(WalkthroughTableEntry.ID, walkthrough.id)
        values.put(WalkthroughTableEntry.SCENARIO_ID, walkthrough.scenarioId)
        values.put(WalkthroughTableEntry.STAKEHOLDER_ID, walkthrough.stakeholderId)
        values.put(WalkthroughTableEntry.OWNER, walkthrough.owner)
        values.put(WalkthroughTableEntry.XML_DATA, walkthrough.getXml())
        return insert(WalkthroughTableEntry.TABLE_NAME, WalkthroughTableEntry.ID, walkthrough.id, values)
    }

    /** READ     **
     ** FROM     **
     ** DATABASE **/
    fun readByteArray(key: String, valueIfNull: ByteArray): ByteArray {
        val db = dbHelper.readableDatabase
        val cursor = db.query(DataTableEntry.TABLE_NAME, arrayOf(DataTableEntry.VALUE), DataTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var bytes: ByteArray? = null
        if (cursor.moveToFirst()) {
            bytes = cursor.getBlob(0)
        }
        cursor.close()
        return ObjectHelper.nvl(bytes, valueIfNull)
    }

    fun readString(key: String, valueIfNull: String): String {
        val db = dbHelper.readableDatabase
        val cursor = db.query(TextTableEntry.TABLE_NAME, arrayOf(TextTableEntry.VALUE), TextTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var d: String? = null
        if (cursor.moveToFirst()) {
            d = cursor.getString(0)
        }
        cursor.close()
        return ObjectHelper.nvl(d, valueIfNull)
    }

    fun readBoolean(key: String, valueIfNull: Boolean): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var b: Int? = null
        if (cursor.moveToFirst()) {
            b = cursor.getInt(0)
        }
        cursor.close()
        return ObjectHelper.nvl(NumberHelper.nvl(b, 0) == 1, valueIfNull)
    }

    fun readShort(key: String, valueIfNull: Short): Short {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var s: Short? = null
        if (cursor.moveToFirst()) {
            s = cursor.getShort(0)
        }
        cursor.close()
        return NumberHelper.nvl(s, valueIfNull)
    }

    fun readInt(key: String, valueIfNull: Int): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var i: Int? = null
        if (cursor.moveToFirst()) {
            i = cursor.getInt(0)
        }
        cursor.close()
        return NumberHelper.nvl(i, valueIfNull)
    }

    fun readLong(key: String, valueIfNull: Long): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var l: Long? = null
        if (cursor.moveToFirst()) {
            l = cursor.getLong(0)
        }
        cursor.close()
        return NumberHelper.nvl(l, valueIfNull)
    }

    fun readFloat(key: String, valueIfNull: Float): Float {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var f: Float? = null
        if (cursor.moveToFirst()) {
            f = cursor.getFloat(0)
        }
        cursor.close()
        return NumberHelper.nvl(f, valueIfNull)
    }

    fun readDouble(key: String, valueIfNull: Double): Double {
        val db = dbHelper.readableDatabase
        val cursor = db.query(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var d: Double? = null
        if (cursor.moveToFirst()) {
            d = cursor.getDouble(0)
        }
        cursor.close()
        return NumberHelper.nvl(d, valueIfNull)
    }

    fun readProjects(): List<Project> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(ProjectTableEntry.TABLE_NAME, arrayOf(ProjectTableEntry.ID, ProjectTableEntry.CREATOR, ProjectTableEntry.TITLE, ProjectTableEntry.DESCRIPTION), ONE + EQ + ONE, null, null, null, null, null)
        val projects = ArrayList<Project>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val creator = cursor.getString(1)
                val title = cursor.getString(2)
                val description = cursor.getString(3)
                val project = Project.ProjectBuilder(id, creator, title, description).build()
                project.changeTimeMs = readVersioning(id)
                projects.add(project)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return projects
    }

    fun readProject(key: String, valueIfNull: Project, fullLoad: Boolean = false): Project {
        val db = dbHelper.readableDatabase
        val cursor = db.query(ProjectTableEntry.TABLE_NAME, arrayOf(ProjectTableEntry.ID, ProjectTableEntry.CREATOR, ProjectTableEntry.TITLE, ProjectTableEntry.DESCRIPTION), ProjectTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val creator = cursor.getString(1)
            val title = cursor.getString(2)
            val description = cursor.getString(3)
            val projectBuilder = Project.ProjectBuilder(id, creator, title, description)
            if (fullLoad) {
                projectBuilder.addStakeholders(stakeholder = *readStakeholders(projectBuilder.build()).toTypedArray())
                projectBuilder.addScenarios(scenario = *readScenarios(projectBuilder.build(), true).toTypedArray())
            }
            val project = projectBuilder.build()
            project.changeTimeMs = readVersioning(id)
            return project
        }
        cursor.close()
        return valueIfNull
    }

    fun readStakeholders(project: Project): List<Stakeholder> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(StakeholderTableEntry.TABLE_NAME, arrayOf(StakeholderTableEntry.ID, StakeholderTableEntry.NAME, StakeholderTableEntry.DESCRIPTION), StakeholderTableEntry.PROJECT_ID + LIKE + QUOTES + project.id + QUOTES, null, null, null, null, null)
        val stakeholders = ArrayList<Stakeholder>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val name = cursor.getString(1)
                val description = cursor.getString(2)
                val stakeholder = Stakeholder.StakeholderBuilder(id, project, name, description).build()
                stakeholder.changeTimeMs = readVersioning(id)
                stakeholders.add(stakeholder)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return stakeholders
    }

    fun readStakeholders(key: String, valueIfNull: Stakeholder): Stakeholder {
        val db = dbHelper.readableDatabase
        val cursor = db.query(StakeholderTableEntry.TABLE_NAME, arrayOf(StakeholderTableEntry.ID, StakeholderTableEntry.PROJECT_ID, StakeholderTableEntry.NAME, StakeholderTableEntry.DESCRIPTION), StakeholderTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val projectId = cursor.getString(1)
            val name = cursor.getString(2)
            val description = cursor.getString(3)
            val stakeholder = Stakeholder.StakeholderBuilder(id, projectId, name, description).build()
            stakeholder.changeTimeMs = readVersioning(id)
            return stakeholder
        }
        cursor.close()
        return valueIfNull
    }

    fun readObject(key: String, valueIfNull: AbstractObject, fullLoad: Boolean = false): AbstractObject {
        val db = dbHelper.readableDatabase
        val cursor = db.query(ObjectTableEntry.TABLE_NAME, arrayOf(ObjectTableEntry.ID, ObjectTableEntry.SCENARIO_ID, ObjectTableEntry.NAME, ObjectTableEntry.DESCRIPTION, ObjectTableEntry.IS_RESOURCE), ObjectTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val scenarioId = cursor.getString(1)
            val name = cursor.getString(2)
            val description = cursor.getString(3)
            val isResource = cursor.getBoolean(4)
            val objectBuilder: AbstractObject.AbstractObjectBuilder
            if (isResource){
                objectBuilder = Resource.ResourceBuilder(id, scenarioId, name, description)
            }else{
                objectBuilder = ContextObject.ContextObjectBuilder(id, scenarioId, name, description)
            }
            if (fullLoad) {
                objectBuilder.addAttributes(attributes = *readAttributes(id).toTypedArray())
            }
            val abstractObject = objectBuilder.build()
            abstractObject.changeTimeMs = readVersioning(id)
            return abstractObject
        }
        cursor.close()
        return valueIfNull
    }

    fun readObjects(scenario: Scenario, fullLoad: Boolean = false): List<AbstractObject> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(ObjectTableEntry.TABLE_NAME, arrayOf(ObjectTableEntry.ID, ObjectTableEntry.NAME, ObjectTableEntry.DESCRIPTION, ObjectTableEntry.IS_RESOURCE), ObjectTableEntry.SCENARIO_ID + LIKE + QUOTES + scenario.id + QUOTES, null, null, null, null, null)
        val objects = ArrayList<AbstractObject>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val name = cursor.getString(1)
                val description = cursor.getString(2)
                val isResource = cursor.getBoolean(3)
                val objectBuilder: AbstractObject.AbstractObjectBuilder
                if (isResource){
                    objectBuilder = Resource.ResourceBuilder(id, scenario, name, description).configure(
                            readDouble(MIN_IDENTIFIER.plus(id),0.0),
                            readDouble(MAX_IDENTIFIER.plus(id),0.0),
                            readDouble(INIT_IDENTIFIER.plus(id),0.0)
                    )
                }else{
                    objectBuilder = ContextObject.ContextObjectBuilder(id, scenario, name, description)
                }
                if (fullLoad) {
                    objectBuilder.addAttributes(attributes = *readAttributes(id).toTypedArray())
                }
                val abstractObject = objectBuilder.build()
                abstractObject.changeTimeMs = readVersioning(id)
                objects.add(abstractObject)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return objects
    }

    fun readAttribute(key: String, valueIfNull: Attribute): Attribute {
        val db = dbHelper.readableDatabase
        val cursor = db.query(AttributeTableEntry.TABLE_NAME, arrayOf(AttributeTableEntry.ID, AttributeTableEntry.KEY, AttributeTableEntry.VALUE), AttributeTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val scenarioId = cursor.getString(1)
            val name = cursor.getString(2)
            val description = cursor.getString(3)
            val attribute = Attribute.AttributeBuilder(id, scenarioId, name, description).build()
            attribute.changeTimeMs = readVersioning(id)
            return attribute
        }
        cursor.close()
        return valueIfNull
    }

    fun readAttributes(refId: String, type: String? = null): List<Attribute> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(AttributeTableEntry.TABLE_NAME, arrayOf(AttributeTableEntry.ID, AttributeTableEntry.KEY, AttributeTableEntry.VALUE), (AttributeTableEntry.REF_ID + LIKE + QUOTES + refId + QUOTES) + if (type == null) "" else (AND + AttributeTableEntry.TYPE + LIKE + QUOTES + type + QUOTES), null, null, null, null, null)
        val attributes = ArrayList<Attribute>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val key = cursor.getString(1)
                val value = cursor.getString(2)
                val attribute = Attribute.AttributeBuilder(id, refId, key, value).withAttributeType(type).build()
                attribute.changeTimeMs = readVersioning(id)
                attributes.add(attribute)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return attributes
    }

    fun readScenario(key: String, valueIfNull: Scenario, fullLoad: Boolean = false): Scenario {
        val db = dbHelper.readableDatabase
        val cursor = db.query(ScenarioTableEntry.TABLE_NAME, arrayOf(ScenarioTableEntry.ID, ScenarioTableEntry.PROJECT_ID, ScenarioTableEntry.TITLE, ScenarioTableEntry.INTRO, ScenarioTableEntry.OUTRO), ScenarioTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val projectId = cursor.getString(1)
            val title = cursor.getString(2)
            val intro = cursor.getString(3)
            val outro = cursor.getString(4)
            val scenarioBuilder = Scenario.ScenarioBuilder(id, projectId, title, intro, outro)
            if (fullLoad) {
                scenarioBuilder.addObjects(obj = *readObjects(scenarioBuilder.build(), true).toTypedArray())
                scenarioBuilder.addPaths(path = *readPaths(scenarioBuilder.build(), true).toTypedArray())
            }
            val scenario = scenarioBuilder.build()
            scenario.changeTimeMs = readVersioning(id)
            return scenario
        }
        cursor.close()
        return valueIfNull
    }

    fun readScenarios(project: Project, fullLoad: Boolean = false): List<Scenario> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(ScenarioTableEntry.TABLE_NAME, arrayOf(ScenarioTableEntry.ID, ScenarioTableEntry.PROJECT_ID, ScenarioTableEntry.TITLE, ScenarioTableEntry.INTRO, ScenarioTableEntry.OUTRO), ScenarioTableEntry.PROJECT_ID + LIKE + QUOTES + project.id + QUOTES, null, null, null, null, null)
        val scenarios = ArrayList<Scenario>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val projectId = cursor.getString(1)
                val title = cursor.getString(2)
                val intro = cursor.getString(3)
                val outro = cursor.getString(4)
                val scenarioBuilder = Scenario.ScenarioBuilder(id, projectId, title, intro, outro)
                if (fullLoad) {
                    scenarioBuilder.addObjects(obj = *readObjects(scenarioBuilder.build(), true).toTypedArray())
                }
                val scenario = scenarioBuilder.build()
                scenario.changeTimeMs = readVersioning(id)
                scenarios.add(scenario)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return scenarios
    }

    fun readPaths(scenario: Scenario, fullLoad: Boolean = false): List<Path> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(PathTableEntry.TABLE_NAME, arrayOf(PathTableEntry.ID, PathTableEntry.STAKEHOLDER_ID, PathTableEntry.LAYER), PathTableEntry.SCENARIO_ID + LIKE + QUOTES + scenario.id + QUOTES, null, null, null, null, null)
        val paths = ArrayList<Path>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val stakeholderId = cursor.getString(1)
                val layer = cursor.getInt(2)
                val path = Path.PathBuilder(id, scenario.id, readStakeholders(stakeholderId, NullHelper.get(Stakeholder::class)), layer).build()
                if (fullLoad) {
                    for (element in readElements(path, true)) {
                        path.add(element)
                    }
                }
                path.changeTimeMs = readVersioning(id)
                paths.add(path)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return paths
    }

    fun readPath(pathId: String, valueIfNull: Path, fullLoad: Boolean = false): Path {
        val db = dbHelper.readableDatabase
        val cursor = db.query(PathTableEntry.TABLE_NAME, arrayOf(PathTableEntry.SCENARIO_ID, PathTableEntry.STAKEHOLDER_ID, PathTableEntry.LAYER), PathTableEntry.ID + LIKE + QUOTES + pathId + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val stakeholderId = cursor.getString(1)
            val layer = cursor.getInt(2)
            val path = Path.PathBuilder(pathId, id, readStakeholders(stakeholderId, NullHelper.get(Stakeholder::class)), layer).build()
            if (fullLoad) {
                for (element in readElements(path, true)) {
                    path.add(element)
                }
            }
            path.changeTimeMs = readVersioning(id)
            return path
        }
        cursor.close()
        return valueIfNull
    }

    @Suppress("UNCHECKED_CAST")
    fun readElements(path: Path, fullLoad: Boolean = false): List<IElement> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(ElementTableEntry.TABLE_NAME, arrayOf(ElementTableEntry.ID, ElementTableEntry.PREV_ID, ElementTableEntry.TYPE, ElementTableEntry.TITLE, ElementTableEntry.TEXT), ElementTableEntry.PATH_ID + LIKE + QUOTES + path.id + QUOTES, null, null, null, null, null)
        val elements = ArrayList<IElement>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val prevId = cursor.getString(1)
                val type = cursor.getString(2)
                val title = cursor.getString(3)
                val text = cursor.getString(4)
                when (type) {
                    TYPE_STANDARD_STEP -> {
                        val step = StandardStep(id, prevId, path.id).withText(text).withTitle(title)
                        if (fullLoad) {
                            for (linkAttribute in readAttributes(id, TYPE_OBJECT)) {
                                step.withObject(readObject(linkAttribute.key as String, NullHelper.get(AbstractObject::class)))
                            }
                        }
                        step.changeTimeMs = readVersioning(id)
                        readWhatIfs(id, step)
                        elements.add(step)
                    }
                    TYPE_BUTTON_TRIGGER -> {
                        val trigger = ButtonTrigger(id, prevId, path.id).withButtonLabel(title)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_IF_ELSE_TRIGGER -> {
                        val trigger = IfElseTrigger(id, prevId, path.id,text,title)
                        var readByteArray = readByteArray(HASH_MAP_OPTIONS_IDENTIFIER.plus(id), byteArrayOf())
                        if (!readByteArray.isEmpty()){
                            trigger.withPathOptions(
                                    ObjectHelper.nvl(
                                            DataHelper.toObject(readByteArray,HashMap::class), HashMap<Int,String>()) as HashMap<Int, String>)
                        }
                        readByteArray = readByteArray(HASH_MAP_LINK_IDENTIFIER.plus(id), byteArrayOf())
                        if (!readByteArray.isEmpty()){
                            trigger.withOptionLayerLink(
                                    ObjectHelper.nvl(
                                            DataHelper.toObject(readByteArray,HashMap::class), HashMap<Int,Int>()) as HashMap<Int, Int>)
                        }
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    else -> {
                        deleteElement(id)
                    }
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return elements
    }

    @Suppress("UNCHECKED_CAST")
    private fun readWhatIfs(id: String?, step: AbstractStep) {
        val readByteArray = readByteArray(ARRAY_LIST_WHAT_IF_IDENTIFIER.plus(id), byteArrayOf())
        if (!readByteArray.isEmpty()) {
            step.withWhatIfs(
                    ObjectHelper.nvl(
                            DataHelper.toObject(readByteArray, ArrayList::class), ArrayList<String>()) as ArrayList<String>)
        }
    }

    fun readWalkthrough(key: String, valueIfNull: Walkthrough): Walkthrough {
        val db = dbHelper.readableDatabase
        val cursor = db.query(WalkthroughTableEntry.TABLE_NAME, arrayOf(WalkthroughTableEntry.ID, WalkthroughTableEntry.SCENARIO_ID, WalkthroughTableEntry.OWNER, WalkthroughTableEntry.STAKEHOLDER_ID, WalkthroughTableEntry.XML_DATA), WalkthroughTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val owner = cursor.getString(1)
            val scenarioId = cursor.getString(2)
            val stakeholderId = cursor.getString(3)
            val xml = cursor.getString(4)
            val walkthrough = Walkthrough.WalkthroughBuilder(id, scenarioId, owner, stakeholderId).withXml(xml).build()
            walkthrough.changeTimeMs = readVersioning(id)
            return walkthrough
        }
        cursor.close()
        return valueIfNull
    }

    fun readWalkthroughs(key: String?): List<Walkthrough> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(WalkthroughTableEntry.TABLE_NAME, arrayOf(WalkthroughTableEntry.ID, WalkthroughTableEntry.SCENARIO_ID, WalkthroughTableEntry.OWNER, WalkthroughTableEntry.STAKEHOLDER_ID, WalkthroughTableEntry.XML_DATA),  WalkthroughTableEntry.SCENARIO_ID + LIKE + QUOTES + (key ?: ANY)  + QUOTES, null, null, null, null, null)
        val walkthroughs = ArrayList<Walkthrough>()
        if (cursor.moveToFirst()) {
            do {
            val id = cursor.getString(0)
            val owner = cursor.getString(1)
            val scenarioId = cursor.getString(2)
            val stakeholderId = cursor.getString(3)
            val xml = cursor.getString(4)
                val walkthrough = Walkthrough.WalkthroughBuilder(id, scenarioId, owner, stakeholderId).withXml(xml).build()
                walkthrough.changeTimeMs = readVersioning(id)
                walkthroughs.add(walkthrough)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return walkthroughs
    }

    private fun addVersioning(id: String){
        writeLong(VERSIONING_IDENTIFIER.plus(id),System.currentTimeMillis())
    }

    private fun readVersioning(id: String): Long{
        return readLong(VERSIONING_IDENTIFIER.plus(id),0)
    }

    private fun deleteVersioning(id: String){
        deleteNumber(VERSIONING_IDENTIFIER.plus(id))
    }

    /** DELETE     **
     ** FROM     **
     ** DATABASE **/
    fun deleteNumber(key: String) {
        delete(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key)
    }

    fun deleteString(key: String) {
        delete(TextTableEntry.TABLE_NAME, TextTableEntry.KEY, key)
    }

    fun deleteData(key: String) {
        delete(DataTableEntry.TABLE_NAME, DataTableEntry.KEY, key)
    }

    fun deleteProject(key: String) {
        delete(ProjectTableEntry.TABLE_NAME, ProjectTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteStakeholder(key: String) {
        delete(StakeholderTableEntry.TABLE_NAME, StakeholderTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteObject(key: String) {
        delete(ObjectTableEntry.TABLE_NAME, ObjectTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteAttribute(key: String) {
        delete(AttributeTableEntry.TABLE_NAME, AttributeTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteAttributeByRefId(key: String) {
        delete(AttributeTableEntry.TABLE_NAME, AttributeTableEntry.REF_ID, key)
        deleteVersioning(key)
    }

    fun deleteAttributeByKey(key: String) {
        delete(AttributeTableEntry.TABLE_NAME, AttributeTableEntry.KEY, key)
        deleteVersioning(key)
    }

    fun deleteScenario(key: String) {
        delete(ScenarioTableEntry.TABLE_NAME, ScenarioTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deletePath(key: String) {
        delete(PathTableEntry.TABLE_NAME, PathTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteElement(key: String) {
        delete(ElementTableEntry.TABLE_NAME, ElementTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteWalkthrough(key: String) {
        delete(WalkthroughTableEntry.TABLE_NAME, WalkthroughTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun truncateStrings() {
        truncate(TextTableEntry.TABLE_NAME)
    }

    fun truncateNumbers() {
        truncate(NumberTableEntry.TABLE_NAME)
    }

    fun truncateData() {
        truncate(DataTableEntry.TABLE_NAME)
    }

    fun truncateProjects() {
        truncate(ProjectTableEntry.TABLE_NAME)
    }

    fun truncateStakeholders() {
        truncate(StakeholderTableEntry.TABLE_NAME)
    }

    fun truncateObjects() {
        truncate(ObjectTableEntry.TABLE_NAME)
    }

    fun truncateAttributes() {
        truncate(AttributeTableEntry.TABLE_NAME)
    }

    fun truncateScenarios() {
        truncate(ScenarioTableEntry.TABLE_NAME)
    }

    /** INTERNAL **/
    private fun insert(tableName: String, keyColumn: String, key: String, values: ContentValues): Long {
        delete(tableName, keyColumn, key)
        val db = dbHelper.writableDatabase
        val newRowId: Long
        newRowId = db.insert(tableName, "null", values)
        db.close()
        return newRowId
    }

    private fun delete(tableName: String, keyColumn: String, key: String) {
        val db = dbHelper.writableDatabase
        val selection = keyColumn + LIKE_WILDCARD
        db.delete(tableName, selection, arrayOf(key))
        db.close()
    }

    private fun truncate(tableName: String) {
        val db = dbHelper.writableDatabase
        db.delete(tableName, null, null)
        db.close()
    }

    public fun dropAndRecreateTable(table: String) {
        val collectTables = collectTables(table)
        if (!collectTables.isEmpty()) {
            for (statement in collectTables) {
                val db = dbHelper.writableDatabase
                db.execSQL(statement)
                db.close()
            }
        }
    }

}