package uzh.scenere.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Spinner
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.BUNDLE_SCENARIO
import uzh.scenere.const.Constants.Companion.SIMPLE_LOOKUP
import uzh.scenere.datamodel.*
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.ObjectHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getStringValue
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButton.SwipeButtonExecution


class ObjectsActivity : AbstractManagementActivity() {

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_objects
    }
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_objects
    }

    enum class ObjectMode{
        VIEW, EDIT, CREATE, ATTRIBUTES
    }
    private var objectsMode: ObjectMode = ObjectMode.VIEW
    override fun isInViewMode(): Boolean {
        return objectsMode == ObjectMode.VIEW
    }

    override fun isInEditMode(): Boolean {
        return objectsMode == ObjectMode.EDIT
    }

    override fun isInAddMode(): Boolean {
        return objectsMode == ObjectMode.CREATE
    }

    override fun resetEditMode() {
        activeObject = null
        isResourceSpinner = null
        objectsMode = ObjectMode.VIEW
    }

    private val inputLabelName = "Object Name"
    private val inputLabelDescription = "Object Description"
    private val inputLabelResource = "Resource"
    private var activeScenario: Scenario? = null
    private var activeObject: AbstractObject? = null
    private var isResourceSpinner: View? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeScenario = intent.getSerializableExtra(BUNDLE_SCENARIO) as Scenario
        creationButton =
                SwipeButton(this,"Create New Object")
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel),ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        .setButtonStates(false,true,false,false)
                        .setButtonIcons(R.string.icon_null,R.string.icon_edit,null,null,R.string.icon_object)
                        .setFirstPosition()
                        .updateViews(true )
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        getContentHolderLayout().addView(creationButton)
        createTitle("",getContentHolderLayout())
        for (obj in DatabaseHelper.getInstance(applicationContext).readBulk(AbstractObject::class, activeScenario)){
            addObjectToList(obj)
        }
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_objects),fontAwesome)
        resetToolbar()
    }

    private fun addObjectToList(obj: AbstractObject) {
        val swipeButton = SwipeButton(this, obj.name)
                .setColors(ContextCompat.getColor(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit, R.string.icon_attributes, R.string.icon_null, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, true, false)
                .updateViews(true)
        swipeButton.dataObject = obj
        swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Attribute::class,obj.id).size,null)
        swipeButton.setExecutable(generateObjectExecutable(swipeButton, obj))
        getContentHolderLayout().addView(swipeButton)
    }

    private fun generateCreationExecutable(button: SwipeButton, obj: AbstractObject? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execRight() {
                activeButton = button
                openInput(ObjectMode.CREATE)
            }
        }
    }

    private fun generateObjectExecutable(button: SwipeButton, obj: AbstractObject? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execLeft() {
                if (obj!=null){
                    removeObject(obj,true)
                    showDeletionConfirmation(obj.name)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(ObjectMode.EDIT,obj)
            }
            override fun execUp() {
                activeButton = button
                openInput(ObjectMode.ATTRIBUTES,obj)
            }
            override fun execReset() {
                resetEditMode()
            }
        }
    }

    override fun createEntity() {
        val name = inputMap[inputLabelName]!!.getStringValue()
        val introduction = inputMap[inputLabelDescription]!!.getStringValue()
        var isResource = false
        if (isResourceSpinner != null){
            val spinner = searchForLayout(isResourceSpinner!!, Spinner::class)
            isResource = spinner?.selectedItem.toString()=="True"
        }
        val objectBuilder: AbstractObject.AbstractObjectBuilder?
        if (isResource){
            objectBuilder = Resource.ResourceBuilder(activeScenario!!, name, introduction).configure(
            inputMap[min]!!.getStringValue().toDouble(),
            inputMap[max]!!.getStringValue().toDouble(),
            inputMap[init]!!.getStringValue().toDouble())
        }else{
            objectBuilder = ContextObject.ContextObjectBuilder(activeScenario!!, name, introduction)
        }
        if (activeObject != null){
            removeObject(activeObject!!)
            objectBuilder.copyId(activeObject!!)
        }
        val obj = objectBuilder.build()
        DatabaseHelper.getInstance(applicationContext).write(obj.id,obj)
        addObjectToList(obj)
    }

    private fun openInput(objectsMode: ObjectMode, obj: AbstractObject? = null) {
        activeObject = obj
        this.objectsMode = objectsMode
        when(objectsMode){
            ObjectMode.VIEW -> {}//NOP
            ObjectMode.EDIT, ObjectMode.CREATE -> {
                cleanInfoHolder(if (activeObject==null) getString(R.string.objects_create) else getString(R.string.objects_edit))
                getInfoContentWrap().addView(createLine(inputLabelName,LineInputType.SINGLE_LINE_EDIT, obj?.name))
                isResourceSpinner = createLine(inputLabelResource, LineInputType.LOOKUP, SIMPLE_LOOKUP, if (ObjectHelper.nvl(obj?.isResource,false)) arrayOf("True", "False") else arrayOf("False", "True"), { execResourceStateChanged() })
                getInfoContentWrap().addView(isResourceSpinner)
                getInfoContentWrap().addView(createLine(inputLabelDescription, LineInputType.MULTI_LINE_EDIT, obj?.description))
            }
            ObjectMode.ATTRIBUTES -> {
                val intent = Intent(this, AttributesActivity::class.java)
                intent.putExtra(Constants.BUNDLE_OBJECT, activeObject)
                startActivity(intent)
                return
            }
        }
        execMorphInfoBar(InfoState.MAXIMIZED)
    }

    private val min = "Min"
    private val max = "Maximum"
    private val init = "Initial"
    private var minResourceLayout: View? = null
    private var maxResourceLayout: View? = null
    private var initResourceLayout: View? = null
    private fun execResourceStateChanged(){
        var isResource = false
        if (isResourceSpinner != null){
            val spinner = searchForLayout(isResourceSpinner!!, Spinner::class)
            isResource = spinner?.selectedItem.toString()=="True"
        }
        if (isResource){
            val resource: Resource? = if (activeObject is Resource) activeObject as Resource else null
            minResourceLayout = createLine(min, LineInputType.NUMBER_EDIT,if (resource?.min == null) null else resource.min.toString())
            maxResourceLayout = createLine(max, LineInputType.NUMBER_EDIT,if (resource?.max == null) null else resource.max.toString())
            initResourceLayout = createLine(init, LineInputType.NUMBER_EDIT,if (resource?.init == null) null else resource.init.toString())
            getInfoContentWrap().addView(minResourceLayout,getInfoContentWrap().childCount-1)
            getInfoContentWrap().addView(maxResourceLayout,getInfoContentWrap().childCount-1)
            getInfoContentWrap().addView(initResourceLayout,getInfoContentWrap().childCount-1)
        }else{
            getInfoContentWrap().removeView(minResourceLayout)
            getInfoContentWrap().removeView(maxResourceLayout)
            getInfoContentWrap().removeView(initResourceLayout)
            minResourceLayout = null
            maxResourceLayout = null
            initResourceLayout = null
            inputMap.remove(min)
            inputMap.remove(max)
            inputMap.remove(init)
        }
    }

    private fun removeObject(obj: AbstractObject, dbRemoval: Boolean = false) {
        for (viewPointer in 0 until getContentHolderLayout().childCount){
            if (getContentHolderLayout().getChildAt(viewPointer) is SwipeButton &&
                    (getContentHolderLayout().getChildAt(viewPointer) as SwipeButton).dataObject == obj){
                getContentHolderLayout().removeViewAt(viewPointer)
                if (dbRemoval) {
                    DatabaseHelper.getInstance(applicationContext).delete(obj.id, AbstractObject::class)
                }
                return
            }
        }
    }

    override fun execDoAdditionalCheck(): Boolean {
        val nameField = inputMap[inputLabelName] ?: return true
        if (StringHelper.hasText(nameField.getStringValue())) {
            if (objectsMode == ObjectMode.EDIT){
                return true
            }
            for (v in 0 until getContentHolderLayout().childCount) {
                if ((getContentHolderLayout().getChildAt(v) is SwipeButton) && (((getContentHolderLayout().getChildAt(v)) as SwipeButton).getText() == nameField.getStringValue())) {
                    notify(getString(R.string.objects_similar_alert))
                    return false
                }
            }
        }
        return true
    }

    override fun onToolbarCenterRightClicked() {
        if (!isInputOpen()) {
            val intent = Intent(this, GlossaryActivity::class.java)
            intent.putExtra(Constants.BUNDLE_GLOSSARY_TOPIC, "Object")
            intent.putExtra(Constants.BUNDLE_GLOSSARY_ADDITIONAL_TOPICS, arrayOf("Resource","Attribute"))
            startActivity(intent)
        }
    }
}