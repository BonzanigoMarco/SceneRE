package uzh.scenere.activities

import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import uzh.scenere.R
import android.widget.Toast
import android.widget.Toolbar
import kotlinx.android.synthetic.main.activity_cockpit.*
import kotlinx.android.synthetic.main.activity_projects.*
import uzh.scenere.views.SwipeButton


class CockpitActivity : AbstractBaseActivity() {
    private var mTopToolbar: Toolbar? = null

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_cockpit
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mTopToolbar = findViewById(R.id.cockpit_toolbar);
        setSupportActionBar(mTopToolbar);
        cockpit_linear_layout_holder.addView(
                SwipeButton(this,"Create New Project")
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(Color.WHITE, Color.GRAY)
                        .setButtonStates(false,true,false,false)
                        .updateViews()
        )
    }

    //enable bluetooth, wifi, nfc, sensors
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_cockpit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.getItemId()


        if (id == R.id.action_favorite) {
            Toast.makeText(applicationContext, "Action clicked", Toast.LENGTH_LONG).show()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}