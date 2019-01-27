package uzh.scenere.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.BUNDLE_SCENARIO
import uzh.scenere.datamodel.Attribute
import uzh.scenere.datamodel.Object
import uzh.scenere.datamodel.Scenario
import uzh.scenere.helpers.DatabaseHelper
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
        VIEW, EDIT_CREATE, ATTRIBUTES
    }
    private var objectsMode: ObjectMode = ObjectMode.VIEW
    override fun isInViewMode(): Boolean {
        return objectsMode == ObjectMode.VIEW
    }
    override fun isInEditMode(): Boolean {
        return objectsMode == ObjectMode.EDIT_CREATE
    }
    override fun resetEditMode() {
        activeObject = null
        objectsMode = ObjectMode.VIEW
    }

    private val inputLabelName = "Object Name"
    private val inputLabelDescription = "Object Description"
    private var activeScenario: Scenario? = null
    private var activeObject: Object? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeScenario = intent.getSerializableExtra(BUNDLE_SCENARIO) as Scenario
        creationButton =
                SwipeButton(this,"Create New Object")
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(Color.WHITE,Color.GRAY)
                        .setButtonStates(false,true,false,false)
                        .setButtonIcons(R.string.icon_null,R.string.icon_edit,null,null,R.string.icon_object)
                        .updateViews(true )
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        scroll_holder_linear_layout_holder.addView(creationButton)
        createTitle("",scroll_holder_linear_layout_holder)
        for (obj in DatabaseHelper.getInstance(applicationContext).readBulk(Object::class, activeScenario)){
            addObjectToList(obj)
        }
        scroll_holder_text_info_title.text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_objects),fontAwesome)
        customizeToolbarText(resources.getText(R.string.icon_back).toString(),null,getLockIcon(),null,null)
    }

    private fun addObjectToList(obj: Object) {
        val swipeButton = SwipeButton(this, obj.name)
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit, R.string.icon_info, R.string.icon_null, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, true, false)
                .updateViews(true)
        swipeButton.dataObject = obj
        swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Attribute::class,obj).size,null)
        swipeButton.setExecutable(generateObjectExecutable(swipeButton, obj))
        scroll_holder_linear_layout_holder.addView(swipeButton)
    }

    private fun generateCreationExecutable(button: SwipeButton, obj: Object? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execRight() {
                activeButton = button
                openInput(ObjectMode.EDIT_CREATE)
            }
        }
    }

    private fun generateObjectExecutable(button: SwipeButton, obj: Object? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execLeft() {
                if (obj!=null){
                    removeObject(obj)
                    showDeletionConfirmation(obj.name)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(ObjectMode.EDIT_CREATE,obj)
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
        val objectBuilder = Object.ObjectBuilder(activeScenario!!,name, introduction)
        if (activeObject != null){
            removeObject(activeObject!!)
            objectBuilder.copyId(activeObject!!)
        }
        val obj = objectBuilder.build()
        DatabaseHelper.getInstance(applicationContext).write(obj.id,obj)
        addObjectToList(obj)
    }

    private fun openInput(objectsMode: ObjectMode, obj: Object? = null) {
        activeObject = obj
        this.objectsMode = objectsMode
        when(objectsMode){
            ObjectMode.VIEW -> {}//NOP
            ObjectMode.EDIT_CREATE -> {
                cleanInfoHolder(if (activeObject==null) getString(R.string.objects_create) else getString(R.string.objects_edit))
                scroll_holder_text_info_content_wrap.addView(createLine(inputLabelName,LineInputType.SINGLE_LINE_TEXT, obj?.name))
                scroll_holder_text_info_content_wrap.addView(createLine(inputLabelDescription, LineInputType.MULTI_LINE_EDIT, obj?.description))
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

    private fun removeObject(obj: Object) {
        for (viewPointer in 0 until scroll_holder_linear_layout_holder.childCount){
            if (scroll_holder_linear_layout_holder.getChildAt(viewPointer) is SwipeButton &&
                    (scroll_holder_linear_layout_holder.getChildAt(viewPointer) as SwipeButton).dataObject == obj){
                scroll_holder_linear_layout_holder.removeViewAt(viewPointer)
                DatabaseHelper.getInstance(applicationContext).delete(obj.id)
                return
            }
        }
    }

    override fun execDoAdditionalCheck(): Boolean {
        val nameField = inputMap[inputLabelName] ?: return true
        if (StringHelper.hasText(nameField.getStringValue())) {
            for (v in 0 until scroll_holder_linear_layout_holder.childCount) {
                if ((scroll_holder_linear_layout_holder.getChildAt(v) is SwipeButton) && (((scroll_holder_linear_layout_holder.getChildAt(v)) as SwipeButton).getText() == nameField.getStringValue())) {
                    toast("An Object with similar Name already exists.")
                    return false
                }
            }
        }
        return true
    }
}