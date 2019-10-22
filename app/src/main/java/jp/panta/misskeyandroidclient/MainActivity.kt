package jp.panta.misskeyandroidclient

import android.content.Intent
import android.os.Bundle

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import jp.panta.misskeyandroidclient.model.notes.NoteRequest
import jp.panta.misskeyandroidclient.model.notes.NoteType
import jp.panta.misskeyandroidclient.view.message.MessageListFragment
import jp.panta.misskeyandroidclient.view.notes.TabFragment
import jp.panta.misskeyandroidclient.view.notes.TimelineFragment
import jp.panta.misskeyandroidclient.view.notification.NotificationFragment
import jp.panta.misskeyandroidclient.view.search.SearchFragment
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setTheme(R.style.AppThemeDark)

        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        /*val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }*/
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        //replaceTimelineFragment()
        init()

        val miApplication = application as MiApplication
        miApplication.currentConnectionInstanceLiveData.observe(this, Observer {
            init()
        })

        miApplication.isSuccessLoadConnectionInstance.observe(this, Observer {
            if(!it){
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
        })

        bottom_navigation.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.navigation_home ->{
                    setFragment("home")
                    true
                }
                R.id.navigation_search ->{
                    setFragment("search")
                    true
                }
                R.id.navigation_notification ->{
                    setFragment("notification")
                    true
                }
                R.id.navigation_message_list ->{
                    setFragment("message")
                    true
                }
                else -> false
            }


        }
        test()

    }

    private fun init(){
        val ci = (application as MiApplication).currentConnectionInstanceLiveData.value
        if(ci == null){

        }else{
            setFragment("home")
        }
    }

    private fun test(){
        //startActivity(Intent(this, AuthActivity::class.java))
    }

    fun changeTitle(title: String?){
        toolbar.title = title
    }




    //default "home"
    private var currentFragmentTag = "home"
    private fun setFragment(tag: String){
        setBottomNavigationSelectState(tag)

        val ft = supportFragmentManager.beginTransaction()

        val targetFragment = supportFragmentManager.findFragmentByTag(tag)
        val currentFragment = supportFragmentManager.findFragmentByTag(currentFragmentTag)


        //表示しようとしているFragmentが表示(add)したことがない場合
        if(targetFragment == null){
            //supportFragmentManager.
            if(currentFragment != null){
                //currentをhideする
                ft.hide(currentFragment)
            }
            ft.add(R.id.content_main, newFragmentByTag(tag), tag)
            currentFragmentTag = tag
            ft.commit()
            return
        }

        //表示しているFragmentと表示しようとしているFragmentが同じ場合
        if(currentFragmentTag == tag && currentFragment != null){
            ft.commit()
            return
        }

        //表示しているFragmentと表示しようとしているFragmentが別でさらに既に存在している場合
        if(currentFragmentTag != tag && currentFragment != null){
            ft.hide(currentFragment)
            ft.show(targetFragment)
            currentFragmentTag = tag
            ft.commit()
            return
        }

    }

    private fun setBottomNavigationSelectState(tag: String){
        when(tag){
            "home" -> bottom_navigation.menu.findItem(R.id.navigation_home).isChecked = true
            "search" -> bottom_navigation.menu.findItem(R.id.navigation_search).isChecked = true
            "notification" -> bottom_navigation.menu.findItem(R.id.navigation_notification).isChecked = true
            "message" -> bottom_navigation.menu.findItem(R.id.navigation_message_list).isChecked = true
        }
    }

    private fun newFragmentByTag(tag: String): Fragment{
        return when(tag){
            "home" -> TabFragment()
            "search" -> SearchFragment()
            "notification" -> NotificationFragment()
            "message" -> MessageListFragment()
            else -> throw IllegalArgumentException("サポートしていないタグです")
        }
    }


    private fun getCurrentFragment(tag: String): Fragment?{
        return supportFragmentManager.findFragmentByTag(tag)
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }else if(currentFragmentTag != "home"){
            setFragment("home")
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_home -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_tools -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
