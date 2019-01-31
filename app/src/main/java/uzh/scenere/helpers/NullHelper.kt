package uzh.scenere.helpers

import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.steps.AbstractStep
import java.io.Serializable
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class NullHelper private constructor(){
    companion object {
        fun <T: Serializable> get(clazz: KClass<T>): T{
            return when(clazz){
                Project::class -> Project.NullProject() as T
                Stakeholder::class -> Stakeholder.NullStakeholder() as T
                Scenario::class -> Scenario.NullScenario() as T
                Attribute::class -> Attribute.NullAttribute() as T
                Object::class -> Object.NullObject() as T
                Path::class -> Path.NullPath() as T
                AbstractStep::class -> AbstractStep.NullStep() as T
                else -> throw ClassNotFoundException()
            }
        }
    }
}