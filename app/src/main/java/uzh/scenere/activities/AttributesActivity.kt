package uzh.scenere.activities

import android.graphics.Color
import android.os.Bundle
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.datamodel.Attribute
import uzh.scenere.datamodel.AbstractObject
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getStringValue
import uzh.scenere.views.SwipeButton

class AttributesActivity : AbstractManagementActivity() {

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_attributes
    }
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_attributes
    }

    enum class AttributeMode {
        VIEW, EDIT_CREATE
    }
    private var attributesMode: AttributeMode = AttributeMode.VIEW
    override fun isInViewMode(): Boolean {
        return attributesMode == AttributeMode.VIEW
    }
    override fun isInEditMode(): Boolean {
        return attributesMode == AttributeMode.EDIT_CREATE
    }
    override fun resetEditMode() {
        activeAttribute = null
        attributesMode = AttributeMode.VIEW
    }

    private val inputLabelKey = "Attribute Name"
    private val inputLabelValue = "Attribute Description"
    private var activeObject: AbstractObject? = null
    private var activeAttribute: Attribute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeObject = intent.getSerializableExtra(Constants.BUNDLE_OBJECT) as AbstractObject
        creationButton =
                SwipeButton(this, "Create New Attribute")
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(Color.WHITE, Color.GRAY)
                        .setButtonStates(false, true, false, false)
                        .setButtonIcons(R.string.icon_null, R.string.icon_edit, null, null, R.string.icon_info)
                        .setFirstPosition()
                        .updateViews(true)
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        scroll_holder_linear_layout_holder.addView(creationButton)
        createTitle("", scroll_holder_linear_layout_holder)
        for (attribute in DatabaseHelper.getInstance(applicationContext).readBulk(Attribute::class, activeObject!!.id)) {
            addAttributeToList(attribute)
        }
        scroll_holder_text_info_title.text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_attributes), fontAwesome)
        customizeToolbarText(resources.getText(R.string.icon_back).toString(), null, getLockIcon(), null, null)
    }

    private fun addAttributeToList(attribute: Attribute) {
        val swipeButton = SwipeButton(this, attribute.key)
                .setColors(Color.WHITE, Color.GRAY)
                .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit,null,null, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, false, false)
                .updateViews(true)
        swipeButton.dataObject = attribute
        swipeButton.setExecutable(generateAttributeExecutable(swipeButton, attribute))
        scroll_holder_linear_layout_holder.addView(swipeButton)
    }

    private fun generateCreationExecutable(button: SwipeButton, attribute: Attribute? = null): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                activeButton = button
                openInput(AttributeMode.EDIT_CREATE)
            }
        }
    }

    private fun generateAttributeExecutable(button: SwipeButton, attribute: Attribute? = null): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execLeft() {
                if (attribute != null) {
                    removeAttribute(attribute, true)
                    showDeletionConfirmation(attribute.key)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(AttributeMode.EDIT_CREATE, attribute)
            }
            override fun execReset() {
                resetEditMode()
            }
        }
    }

    override fun createEntity() {
        val key = inputMap[inputLabelKey]!!.getStringValue()
        val value = inputMap[inputLabelValue]!!.getStringValue()
        val attributeBuilder = Attribute.AttributeBuilder(activeObject!!.id, key, value)
        if (activeAttribute != null) {
            removeAttribute(activeAttribute!!)
            attributeBuilder.copyId(activeAttribute!!)
        }
        val attribute = attributeBuilder.build()
        DatabaseHelper.getInstance(applicationContext).write(attribute.id, attribute)
        addAttributeToList(attribute)
    }

    private fun openInput(attributesMode: AttributeMode, attribute: Attribute? = null) {
        activeAttribute = attribute
        this.attributesMode = attributesMode
        when (attributesMode) {
            AttributeMode.VIEW -> {}//NOP
            AttributeMode.EDIT_CREATE -> {
                cleanInfoHolder(if (activeAttribute == null) getString(R.string.attributes_create) else getString(R.string.attributes_edit))
                scroll_holder_text_info_content_wrap.addView(createLine(inputLabelKey, LineInputType.SINGLE_LINE_EDIT, attribute?.key))
                scroll_holder_text_info_content_wrap.addView(createLine(inputLabelValue, LineInputType.MULTI_LINE_EDIT, attribute?.value))
            }
        }
        execMorphInfoBar(InfoState.MAXIMIZED)
    }

    private fun removeAttribute(attribute: Attribute, dbRemoval: Boolean = false) {
        for (viewPointer in 0 until scroll_holder_linear_layout_holder.childCount) {
            if (scroll_holder_linear_layout_holder.getChildAt(viewPointer) is SwipeButton &&
                    (scroll_holder_linear_layout_holder.getChildAt(viewPointer) as SwipeButton).dataObject == attribute) {
                scroll_holder_linear_layout_holder.removeViewAt(viewPointer)
                if (dbRemoval){
                    DatabaseHelper.getInstance(applicationContext).delete(attribute.id, Attribute::class)
                }
                return
            }
        }
    }
}