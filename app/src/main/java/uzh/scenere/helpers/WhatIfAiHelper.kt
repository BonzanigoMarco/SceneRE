package uzh.scenere.helpers

import android.content.Context
import uzh.scenere.const.Constants
import uzh.scenere.datamodel.AbstractObject
import uzh.scenere.datamodel.IElement
import uzh.scenere.datamodel.steps.AbstractStep

class WhatIfAiHelper{
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun think(context: Context){
            val startingTime = System.currentTimeMillis()
            val steps = DatabaseHelper.getInstance(context).readBulk(IElement::class, null)
            //Existing Data

            val existingData = HashMap<String,ArrayList<String>>()
            existingData[Constants.OBJECT_TOKEN] = ArrayList<String>()
            existingData["${Constants.ATTRIBUTE_TOKEN}${Constants.OBJECT_TOKEN}"] = ArrayList<String>()
            existingData["${Constants.STAKEHOLDER_1_TOKEN}${Constants.STAKEHOLDER_2_TOKEN}"] = ArrayList<String>()
            existingData[Constants.STAKEHOLDER_1_TOKEN] = ArrayList<String>()
            existingData[Constants.STATIC_TOKEN] = ArrayList<String>()
            val bytes = DatabaseHelper.getInstance(context).read(Constants.WHAT_IF_DATA,ByteArray::class,NullHelper.get(ByteArray::class))
            if (bytes.isNotEmpty()){
                try{
                    existingData.putAll(DataHelper.toObject(bytes,HashMap::class) as HashMap<String,ArrayList<String>>)
                }catch(e: Exception){/*NOP*/}
            }
            //New Data Preparation
            val map = HashMap<String,ArrayList<AbstractObject>>()
            for (step in steps ){
                if (step is AbstractStep &&  !step.whatIfs.isEmpty()){
                    if (!step.objects.isEmpty() ){
                        // Objects Related What-Ifs
                        for (whatIf in step.whatIfs){
                            map[whatIf] = step.objects
                        }
                    }else{
                        // General What-Ifs
                        for (whatIf in step.whatIfs){
                            map[whatIf] = ArrayList()
                        }
                    }
                }
            }
            val endTime = System.currentTimeMillis() - startingTime
            val a = 1
        }
    }
}