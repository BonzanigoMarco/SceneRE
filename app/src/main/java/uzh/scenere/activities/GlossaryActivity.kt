package uzh.scenere.activities

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ScrollView
import kotlinx.android.synthetic.main.activity_glossary.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getColorWithStyle
import uzh.scenere.views.SreScrollView
import uzh.scenere.views.SreTextView
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButtonSortingLayout

class GlossaryActivity: AbstractManagementActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return glossary_root
    }

    override fun isInEditMode(): Boolean {
        return inputOpen
    }

    override fun isInAddMode(): Boolean {
        return inputOpen
    }

    override fun isInViewMode(): Boolean {
        return true
    }

    override fun resetEditMode() {
        //NOP
    }

    override fun createEntity() {
        //NOP
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_null
    }

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_glossary
    }

    override fun resetToolbar() {
        customizeToolbarId(R.string.icon_back,null,null,null,null)
    }

    private val buttonMap = HashMap<String,SwipeButton>()
    private var inputOpen = false
    private var scrollContainer: ScrollView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val topic = intent.getStringExtra(Constants.BUNDLE_GLOSSARY_TOPIC)
        val additional = intent.getStringArrayExtra(Constants.BUNDLE_GLOSSARY_ADDITIONAL_TOPICS)
        buttonMap["Project"] = createGlossaryButton("Project",R.string.icon_project, R.string.glossary_project)
        buttonMap["Stakeholder"] = createGlossaryButton("Stakeholder",R.string.icon_stakeholder, R.string.glossary_stakeholder)
        buttonMap["Scenario"] = createGlossaryButton("Scenario",R.string.icon_scenario, R.string.glossary_scenario)
        buttonMap["Object"] = createGlossaryButton("Object",R.string.icon_object, R.string.glossary_object)
        buttonMap["Attribute"] = createGlossaryButton("Attribute",R.string.icon_attributes, R.string.glossary_attribute)
        buttonMap["Resource"] = createGlossaryButton("Resource",R.string.icon_resource, R.string.glossary_resource)
        buttonMap["Walkthrough"] = createGlossaryButton("Walkthrough",R.string.icon_walkthrough, R.string.glossary_walkthrough)
        buttonMap["Step"] = createGlossaryButton("Step",R.string.icon_step, R.string.glossary_step)
        buttonMap["Trigger"] = createGlossaryButton("Trigger",R.string.icon_trigger, R.string.glossary_trigger)
        buttonMap["Editor"] = createGlossaryButton("Editor",R.string.icon_path_editor, R.string.glossary_editor)

        var button: SwipeButton? = null
        for (entry in buttonMap.entries){
            if (entry.key == topic){
                button = entry.value
                entry.value.setIndividualButtonColors(getColorWithStyle(applicationContext,R.color.srePrimaryWarn),getColorWithStyle(applicationContext,R.color.srePrimaryWarn),getColorWithStyle(applicationContext,R.color.srePrimaryWarn),getColorWithStyle(applicationContext,R.color.srePrimaryWarn),getColorWithStyle(applicationContext,R.color.srePrimaryWarn)).updateViews(false)
            }
            if (additional != null){
                for (add in additional){
                    if (entry.key == add) {
                        entry.value.setIndividualButtonColors(getColorWithStyle(applicationContext,R.color.srePrimaryAttention), getColorWithStyle(applicationContext,R.color.srePrimaryAttention), getColorWithStyle(applicationContext,R.color.srePrimaryAttention), getColorWithStyle(applicationContext,R.color.srePrimaryAttention), getColorWithStyle(applicationContext,R.color.srePrimaryAttention)).updateViews(false)
                    }
                }
            }
            getContentHolderLayout().addView(entry.value)
        }
        if (button != null && getContentHolderLayout() is SwipeButtonSortingLayout){
            (getContentHolderLayout() as SwipeButtonSortingLayout).scrollTo(button)
        }

        scrollContainer = SreScrollView(applicationContext,getInfoContent().parent as ViewGroup)
        (getInfoContent().parent as ViewGroup).addView(scrollContainer)
        getInfoContent().visibility = GONE
        resetToolbar()
    }

    private fun createGlossaryButton(label: String, icon: Int, glossaryText: Int): SwipeButton {
        return SwipeButton(this, label)
                .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                .setButtonStates(false, true, false, false)
                .setButtonIcons(R.string.icon_null, R.string.icon_question, null, null, icon)
                .setExecutable(object: SwipeButton.SwipeButtonExecution{
                    override fun execRight() {
                        inputOpen = true
                        getInfoTitle().text = label
                        val info = SreTextView(applicationContext,scrollContainer,NOTHING)
                        info.text = StringHelper.fromHtml(resources.getString(glossaryText))
                        scrollContainer?.removeAllViews()
                        scrollContainer?.addView(info)
                        execMorphInfoBar(InfoState.MAXIMIZED,100)
                        customizeToolbarId(R.string.icon_back,null,null,null,R.string.icon_cross)
                    }
                })
                .setAutoCollapse(true)
                .updateViews(true)
    }

    override fun onToolbarCenterClicked() {
        //NOP
    }

    override fun onToolbarRightClicked() {
        getInfoTitle().text = null
        getInfoContent().text = null
        execMorphInfoBar(InfoState.MINIMIZED)
        resetToolbar()
        inputOpen = false
        scrollContainer?.removeAllViews()
    }

    override fun getIsFirstScrollUp(): Boolean {
        return false
    }
}